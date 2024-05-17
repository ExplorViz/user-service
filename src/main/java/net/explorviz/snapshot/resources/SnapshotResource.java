package net.explorviz.snapshot.resources;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
   * @param owner
   * @param createdAt
   * @param name
   * @param landscapeToken
   * @param structureData
   * @param configuration
   * @param camera
   * @param annotations
   * @param isShared
   * @param deleteAt
   * @param julius
   * @return
   */
  @POST
  @Authenticated
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create")
//  public Response createNewSnapshot(@QueryParam("owner") final String owner,
//      @QueryParam("createdAt") final Long createdAt, @QueryParam("name") final String name,
//      @QueryParam("landscapeToken") final Document landscapeToken,
//      @QueryParam("structureData") final Document structureData,
//      @QueryParam("configuration") final Document configuration,
//      @QueryParam("camera") final Document camera, @QueryParam("annotations") final Document[] annotations,
//      @QueryParam("isShared") final boolean isShared,
//      @QueryParam("deleteAt") @DefaultValue("0") final Long deleteAt,
//      @QueryParam("julius") Document julius) {
  public Response createNewSnapshot(Snapshot snapshot){

    System.out.println(snapshot.toString());
    if (snapshotService.snapshotExists(snapshot.getOwner(), snapshot.getCreatedAt())) {
      return Response.status(422).build();
    } else {
//      this.snapshotService.createNewSnapshot(owner, createdAt, name, landscapeToken, structureData,
//          configuration, camera, annotations, isShared, deleteAt, julius);
      this.snapshotService.test(snapshot);

      return Response.ok().build();
    }
  }

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

  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Snapshot> getSnapshotByValue(@QueryParam("owner") final String owner) {
    return this.snapshotService.getOwningSnapshots(owner);
  }

}
