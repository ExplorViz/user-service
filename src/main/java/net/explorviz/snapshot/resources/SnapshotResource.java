package net.explorviz.snapshot.resources;

import io.quarkus.security.Authenticated;
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

    System.out.println(snapshot.toString());
    if (snapshotService.snapshotExists(snapshot.getOwner(), snapshot.getCreatedAt())) {
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
      @QueryParam("createdAt") final Long createdAt) {

    int status = this.snapshotService.deleteByValue(owner, createdAt);

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
  public Collection<Snapshot> getSnapshotByValue(@QueryParam("owner") final String owner) {

    Collection<Snapshot> snapshots = this.snapshotService.getOwningSnapshots(owner);
    final long currentTime = System.currentTimeMillis();

    for (Snapshot sn : snapshots) {
      if (sn.getDeleteAt() != 0 && sn.getDeleteAt() < currentTime) {
        this.snapshotService.deleteByValue(sn.getOwner(), sn.getCreatedAt());
      }
    }

    snapshots = this.snapshotService.getOwningSnapshots(owner);

    return snapshots;
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
  @Path("/{owner}/{createdAt}")
  public Snapshot getSnapshot(@PathParam("owner") final String owner, @PathParam("createdAt") final Long createdAt) {

   return this.snapshotService.getSnapshot(owner, createdAt);

  }

}
