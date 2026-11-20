package net.explorviz.snapshot.resources;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.explorviz.auth.CallerGuard;
import net.explorviz.demo.DemoMode;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.service.SnapshotService;
import org.bson.Document;

@Path("snapshot")
@RequestScoped
public class SnapshotResource {

  @Inject DemoMode demoMode;

  @Inject /* default */ CallerGuard callerGuard;

  private final SnapshotService snapshotService;

  public SnapshotResource(final SnapshotService snapshotService) {
    this.snapshotService = snapshotService;
  }

  /**
   * Endpoint to create a snapshot.
   *
   * @param snapshot Retrieved snapshot object.
   * @return Response to the requester.
   */
  @POST
  @Authenticated
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create")
  public Response createNewSnapshot(Snapshot snapshot) {
    if (snapshot == null || snapshot.getOwner() == null || snapshot.getCreatedAt() == null) {
      throw new BadRequestException("Snapshot must have an owner and a creation date");
    }
    this.callerGuard.requireCaller(snapshot.getOwner());

    if (snapshotService.snapshotExists(
        snapshot.getOwner(), snapshot.getCreatedAt(), snapshot.getIsShared())) {
      return Response.status(422).build();
    } else {
      this.snapshotService.createNewSnapshot(snapshot);

      return Response.ok().build();
    }
  }

  /**
   * Endpoint to delete a snapshot.
   *
   * @param owner owner of the snapshot.
   * @param createdAt creation date of the snapshot.
   * @return Response to the requester.
   */
  @DELETE
  @Authenticated
  @Path("delete")
  public Response deleteSnapshot(
      @QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("isShared") final boolean isShared) {
    requireIdentifier(owner, createdAt);
    this.callerGuard.requireCaller(owner);

    int status = this.snapshotService.deleteByValue(owner, createdAt, isShared);

    if (status == 0) {
      return Response.ok().build();
    }

    return Response.status(400).build();
  }

  /**
   * Endpoint to get all snapshots owned by a user.
   *
   * @param owner owner of snapshots.
   * @return Collection of snapshots.
   */
  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public Document getSnapshotByValue(@QueryParam("owner") final String owner) {
    Document tinySnapshots = new Document();
    ArrayList<Document> personalSnapshots = new ArrayList<>();
    ArrayList<Document> sharedSnapshots = new ArrayList<>();
    ArrayList<Document> subscribedSnapshots = new ArrayList<>();

    final String user = owner == null ? this.callerGuard.callerId() : owner;
    this.callerGuard.requireCaller(user);

    final Collection<Snapshot> snapshots =
        user == null || user.isEmpty() ? List.of() : this.snapshotService.getVisibleSnapshots(user);
    final long currentTime = System.currentTimeMillis();

    for (Snapshot sn : snapshots) {

      List<String> subscriberList = null;
      if (sn.getSubscribedUsers() != null) {
        subscriberList = sn.getSubscribedUsers().getList("subscriberList", String.class);
      }
      if (subscriberList == null) {
        subscriberList = new ArrayList<>();
      }

      if (Snapshot.isExpired(sn, currentTime)) {
        this.deleteExpired(sn);
      } else if (user.equals(sn.getOwner())) { // collect all personal and shared snapshots
        if (sn.getIsShared()) {
          sharedSnapshots.add(toSummary(sn));
        } else {
          personalSnapshots.add(toSummary(sn));
        }
      } else if (subscriberList.contains(user)) {
        subscribedSnapshots.add(toSummary(sn));
      }
    }

    tinySnapshots.append("personalSnapshots", personalSnapshots);
    tinySnapshots.append("sharedSnapshots", sharedSnapshots);
    tinySnapshots.append("subscribedSnapshots", subscribedSnapshots);
    return tinySnapshots;
  }

  /**
   * Endpoint to get all snapshots owned by a user.
   *
   * @param owner owner of snapshots.
   * @return Collection of snapshots.
   */
  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/get")
  public Snapshot getSnapshot(
      @QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("isShared") final boolean isShared,
      @QueryParam("subscriber") final String subscriber) {
    requireIdentifier(owner, createdAt);
    final boolean hasSubscriber = subscriber != null && !subscriber.isEmpty();
    // Shared snapshots may be opened by anyone, personal ones only by their owner
    if (!isShared) {
      this.callerGuard.requireCaller(owner);
    }
    if (hasSubscriber) {
      this.callerGuard.requireCaller(subscriber);
    }

    final Snapshot snapshot = this.snapshotService.getSnapshot(owner, createdAt, isShared);
    if (snapshot == null) {
      throw new NotFoundException();
    }
    if (Snapshot.isExpired(snapshot, System.currentTimeMillis())) {
      this.deleteExpired(snapshot);
      throw new NotFoundException();
    }

    if (!demoMode.isEnabled() && isShared && hasSubscriber && !subscriber.equals(owner)) {
      this.snapshotService.addNewSubscriber(owner, createdAt, subscriber);
    }
    return snapshot;
  }

  @PUT
  @Authenticated
  @Path("/subscribe")
  public Response subscribeSnapshot(
      @QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("subscriber") final String subscriber) {
    requireIdentifier(owner, createdAt);
    requireSubscriber(subscriber);
    this.callerGuard.requireCaller(subscriber);
    this.snapshotService.addNewSubscriber(owner, createdAt, subscriber);

    return Response.ok().build();
  }

  @PUT
  @Authenticated
  @Path("/unsubscribe")
  public Response unsubscribeSnapshot(
      @QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("subscriber") final String subscriber) {
    requireIdentifier(owner, createdAt);
    requireSubscriber(subscriber);
    // Subscribers may unsubscribe themselves, owners may remove any subscriber
    if (!this.callerGuard.isCaller(subscriber)) {
      this.callerGuard.requireCaller(owner);
    }
    this.snapshotService.removeSubscriber(owner, createdAt, subscriber);

    return Response.ok().build();
  }

  @PUT
  @Authenticated
  @Path("/share")
  public Response shareSnapshot(
      @QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("deleteAt") final Long deleteAt) {
    requireIdentifier(owner, createdAt);
    this.callerGuard.requireCaller(owner);

    int res =
        this.snapshotService.shareSnapshot(owner, createdAt, deleteAt == null ? 0L : deleteAt);

    if (res == 0) {
      return Response.ok().build();
    } else if (res == 1) {
      // if snapshot already shared return status 222
      return Response.status(222).build();
    } else {
      // if given data doesn't correspond to an existing snapshot return status 400
      return Response.status(400).build();
    }
  }

  private void deleteExpired(final Snapshot snapshot) {
    if (!demoMode.isEnabled()) {
      this.snapshotService.deleteByValue(
          snapshot.getOwner(), snapshot.getCreatedAt(), snapshot.getIsShared());
    }
  }

  private static Document toSummary(final Snapshot sn) {
    return new Document()
        .append("owner", sn.getOwner())
        .append("createdAt", sn.getCreatedAt())
        .append("name", sn.getName())
        .append("landscapeToken", sn.getLandscapeToken());
  }

  private static void requireIdentifier(final String owner, final Long createdAt) {
    if (owner == null || owner.isEmpty() || createdAt == null) {
      throw new BadRequestException("Query parameters 'owner' and 'createdAt' are required");
    }
  }

  private static void requireSubscriber(final String subscriber) {
    if (subscriber == null || subscriber.isEmpty()) {
      throw new BadRequestException("Query parameter 'subscriber' is required");
    }
  }
}
