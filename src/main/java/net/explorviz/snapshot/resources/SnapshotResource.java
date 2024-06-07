package net.explorviz.snapshot.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.User;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.service.SnapshotService;
import org.bson.Document;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import java.util.ArrayList;
import java.util.Collection;

@Path("snapshot")
@RequestScoped
public class SnapshotResource {

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
  public Response createNewSnapshot(Snapshot snapshot){
    if (snapshotService.snapshotExists(snapshot.getOwner(), snapshot.getCreatedAt(),
        snapshot.getIsShared())) {
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
  public Response deleteSnapshot(@QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt, @QueryParam("isShared") final boolean isShared) {

    int status = this.snapshotService.deleteByValue(owner, createdAt, isShared);

    if (status == 0){
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

    Collection<Snapshot> snapshots = this.snapshotService.getAllSnapshots();
    final long currentTime = System.currentTimeMillis();

    for (Snapshot sn : snapshots) {

      ArrayList<String> subscriberList = new ArrayList<>();

      if (!sn.getSubscribedUsers().isEmpty()) {
        subscriberList = (ArrayList<String>) sn.getSubscribedUsers().get("subscriberList");
      }

      if (sn.getDeleteAt() != 0 && sn.getDeleteAt() < currentTime) {

        this.snapshotService.deleteByValue(sn.getOwner(), sn.getCreatedAt(), sn.getIsShared());

      } else if (sn.getOwner().equals(owner)) { // collect all personal and shared snapshots

        JSONObject jsonSnapshot = new JSONObject();
        jsonSnapshot.put("owner", sn.getOwner());
        jsonSnapshot.put("createdAt", sn.getCreatedAt());
        jsonSnapshot.put("name", sn.getName());
        jsonSnapshot.put("landscapeToken", sn.getLandscapeToken());

        if (sn.getIsShared()) {
          sharedSnapshots.add(Document.parse(jsonSnapshot.toJSONString()));
        } else {
          personalSnapshots.add(Document.parse(jsonSnapshot.toJSONString()));
        }

      } else if (subscriberList.contains(owner)) {

        JSONObject jsonSnapshot = new JSONObject();
        jsonSnapshot.put("owner", sn.getOwner());
        jsonSnapshot.put("createdAt", sn.getCreatedAt());
        jsonSnapshot.put("name", sn.getName());
        jsonSnapshot.put("landscapeToken", sn.getLandscapeToken());

        subscribedSnapshots.add(Document.parse(jsonSnapshot.toJSONString()));
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
  public Snapshot getSnapshot(@QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt, @QueryParam("isShared") final boolean isShared,
      @QueryParam("subscriber") final String subscriber) {

    if (isShared && !subscriber.equals(owner)) {
      this.snapshotService.addNewSubscriber(owner, createdAt, subscriber);
    }

   return this.snapshotService.getSnapshot(owner, createdAt, isShared);

  }

  @PUT
  @Authenticated
  @Path("/subscribe")
  public Response subscribeSnapshot(@QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("subscriber") final String subscriber) {
    this.snapshotService.addNewSubscriber(owner, createdAt, subscriber);

    return Response.ok().build();
  }

  @PUT
  @Authenticated
  @Path("/unsubscribe")
  public Response unsubscribeSnapshot(@QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("subscriber") final String subscriber) {
    this.snapshotService.removeSubscriber(owner, createdAt, subscriber);

    return Response.ok().build();
  }

  @PUT
  @Authenticated
  @Path("/share")
  public Response shareSnapshot(@QueryParam("owner") final String owner,
      @QueryParam("createdAt") final Long createdAt, @QueryParam("deleteAt") final Long deleteAt) {

    int res = this.snapshotService.shareSnapshot(owner, createdAt, deleteAt);

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
}
