package net.explorviz.snapshot.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.User;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
    System.out.println(snapshot);

    if (snapshotService.snapshotExists(snapshot.getOwner(), snapshot.getCreatedAt(),
        snapshot.getIsShared())) {
      System.out.println("WARUUUUUUM????");
      return Response.status(422).build();
    } else {
      System.out.println("Called");
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
  public Collection<Document> getSnapshotByValue(@QueryParam("owner") final String owner) {

    Collection<Snapshot> snapshots = this.snapshotService.getOwningSnapshots(owner);
    final long currentTime = System.currentTimeMillis();

    ArrayList<Document> tinySnapshots = new ArrayList<>();

    for (Snapshot sn : snapshots) {
      if (sn.getDeleteAt() != 0 && sn.getDeleteAt() < currentTime) {
        this.snapshotService.deleteByValue(sn.getOwner(), sn.getCreatedAt(), sn.getIsShared());
      } else {
        JSONObject jsonSnapshot = new JSONObject();
        jsonSnapshot.put("owner", sn.getOwner());
        jsonSnapshot.put("createdAt", sn.getCreatedAt());
        jsonSnapshot.put("name", sn.getName());
        jsonSnapshot.put("landscapeToken", sn.getLandscapeToken());

        tinySnapshots.add(Document.parse(jsonSnapshot.toJSONString()));
      }
    }

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
  @Path("/{owner}/{createdAt}/{isShared}")
  public Snapshot getSnapshot(@PathParam("owner") final String owner, @PathParam("createdAt") final Long createdAt, @PathParam("isShared") final boolean isShared) {

   return this.snapshotService.getSnapshot(owner, createdAt, isShared);

  }

}
