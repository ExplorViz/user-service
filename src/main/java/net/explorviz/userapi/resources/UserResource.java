package net.explorviz.userapi.resources;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Null;
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
import net.explorviz.userapi.model.UserAPI;
import net.explorviz.userapi.service.UserAPIService;
import java.util.Collection;

@Path("userapi")
@RequestScoped
public class UserResource {

  private final UserAPIService userAPIService;


  @Inject
  public UserResource(final UserAPIService userAPIService) {
    this.userAPIService = userAPIService;
  }

  /**
   * Endpoint to generate a user.
   *
   * @param uId Id of the user.
   * @param name  Name of the API token.
   * @param token Actual API token with creation and expiration date.
   * @return Generated user.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create")
  public UserAPI createNewUserAPI(@QueryParam("uId") final String uId,
      @QueryParam("name") final String name, @QueryParam("token") final String token,
      @QueryParam("createdAt") final String createdAt,
      @QueryParam("expires") @DefaultValue("0") final Long expires) {

    Long created = 0L;
    try {
      created = Long.valueOf(createdAt);
    } catch (Exception e) {
      System.out.println(e);
    }

    return this.userAPIService.createNewUserAPI(uId, name, token, created, expires);
  }

  /**
   * Endpoint to delete a user.
   *
   * @param uId Id of the user.
   * @param token  The API token.
   * @return Response with empty content.
   */
  @DELETE
  @Authenticated
  @Produces(MediaType.TEXT_PLAIN)
  @Path("delete")
  public Response deleteUser(@PathParam("uid") final String uId,
      @PathParam("token") final String token) {

    int status = this.userAPIService.deleteByValue(uId, token);

    if (status == 0){
      return Response.noContent().build();
    }

    return Response.status(400).build();
  }

  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<UserAPI> getUserByValue(@PathParam("uId") final String uId){
    return this.userAPIService.getOwningTokens(uId);
  }

}
