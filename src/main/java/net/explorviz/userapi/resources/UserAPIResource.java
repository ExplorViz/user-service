package net.explorviz.userapi.resources;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
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
public class UserAPIResource {

  private final UserAPIService userAPIService;


  @Inject
  public UserAPIResource(final UserAPIService userAPIService) {
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
//  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
//  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create")
  public Response createNewUserAPI(@QueryParam("uId") final String uId,
      @QueryParam("name") final String name, @QueryParam("token") final String token,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("expires") @DefaultValue("0") final Long expires) {

    if (userAPIService.tokenExists(uId, token)){
      return Response.status(422).build();
    } else {
      this.userAPIService.createNewUserAPI(uId, name, token, createdAt, expires);

      return Response.ok().build();
    }
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
//  @Produces(MediaType.TEXT_PLAIN
  @Path("delete")
  public Response deleteUser(@QueryParam("uId") final String uId,
      @QueryParam("token") final String token) {

    int status = this.userAPIService.deleteByValue(uId, token);

    if (status == 0){
      return Response.ok().build();
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
