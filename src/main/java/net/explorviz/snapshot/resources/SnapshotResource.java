package net.explorviz.snapshot.resources;

import io.quarkus.panache.common.Page;
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
import java.util.List;
import net.explorviz.auth.CallerGuard;
import net.explorviz.demo.DemoMode;
import net.explorviz.persistence.DuplicateKeys;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.model.SnapshotOverview;
import net.explorviz.snapshot.service.SnapshotService;

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
      return DuplicateKeys.conflict("Snapshot already exists.");
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
   * Endpoint to get the snapshots a user owns or has subscribed to, newest first. Returns complete
   * lists unless a page size is given.
   *
   * @param owner owner of snapshots.
   * @param pageIndex index of the requested page (default = 0).
   * @param pageSize maximum number of snapshots per list.
   * @return the requested page of the personal, shared, and subscribed snapshots.
   */
  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public SnapshotOverview getSnapshotByValue(
      @QueryParam("owner") final String owner,
      @QueryParam("page") final Integer pageIndex,
      @QueryParam("size") final Integer pageSize) {
    final Page page = toPage(pageIndex, pageSize);
    final String user = owner == null ? this.callerGuard.callerId() : owner;
    this.callerGuard.requireCaller(user);

    if (user == null || user.isEmpty()) {
      return new SnapshotOverview(List.of(), List.of(), List.of());
    }
    return this.snapshotService.getVisibleSnapshots(user, page);
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
    if (snapshot == null || Snapshot.isExpired(snapshot, System.currentTimeMillis())) {
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

  private static Page toPage(final Integer index, final Integer size) {
    if (size == null) {
      if (index != null) {
        throw new BadRequestException("Query parameter 'page' requires 'size'");
      }
      return Page.ofSize(Integer.MAX_VALUE);
    }
    final int effectiveIndex = index == null ? 0 : index;
    if (size < 1 || effectiveIndex < 0 || (long) effectiveIndex * size > Integer.MAX_VALUE) {
      throw new BadRequestException("Query parameters 'page' and 'size' are out of range");
    }
    return Page.of(effectiveIndex, size);
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
