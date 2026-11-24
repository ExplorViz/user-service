package net.explorviz.userapi.resources;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import net.explorviz.auth.CallerGuard;
import net.explorviz.demo.DemoMode;
import net.explorviz.persistence.DuplicateKeys;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.service.UserApiService;

@Path("userapi")
@RequestScoped
public class UserApiResource {

  @Inject DemoMode demoMode;

  @Inject /* default */ CallerGuard callerGuard;

  private final UserApiService userApiService;

  @Inject
  public UserApiResource(final UserApiService userApiService) {
    this.userApiService = userApiService;
  }

  /**
   * Endpoint to create a user.
   *
   * @param uid Id of the user.
   * @param name Name of the API token.
   * @param token The API token.
   * @param createdAt The creation date.
   * @param expires The expiration date (default = 0).
   * @return Response to the requester.
   */
  @POST
  @Authenticated
  @Path("create")
  public Response createNewUserApi(
      @QueryParam("uId") final String uid,
      @QueryParam("name") final String name,
      @QueryParam("token") final String token,
      @QueryParam("hostUrl") final String hostUrl,
      @QueryParam("createdAt") final Long createdAt,
      @QueryParam("expires") @DefaultValue("0") final Long expires) {

    if (uid == null || uid.isBlank() || token == null || token.isBlank()) {
      throw new BadRequestException("Query parameters 'uId' and 'token' are required");
    }
    this.callerGuard.requireCaller(uid);

    if (userApiService.tokenExists(uid, token)) {
      return DuplicateKeys.conflict("API token already exists.");
    } else {
      final long effectiveCreatedAt = createdAt == null ? System.currentTimeMillis() : createdAt;
      this.userApiService.createNewUserApi(
          uid, name, token, hostUrl, effectiveCreatedAt, expires == null ? 0L : expires);
      return Response.ok().build();
    }
  }

  /**
   * Endpoint to delete a user.
   *
   * @param uid Id of the user.
   * @param token The API token.
   * @return Response to the requester.
   */
  @DELETE
  @Authenticated
  @Path("delete")
  public Response deleteUser(
      @QueryParam("uId") final String uid, @QueryParam("token") final String token) {
    this.callerGuard.requireCaller(uid);

    int status = this.userApiService.deleteByValue(uid, token);

    if (status == 0) {
      return Response.ok().build();
    }

    return Response.status(400).build();
  }

  /**
   * Endpoint to get all user API tokens of one user.
   *
   * @param uid Id of the user.
   * @return Collection of UserAPI objects.
   */
  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<UserApi> getUserByValue(@QueryParam("uId") final String uid) {
    this.callerGuard.requireCaller(uid);

    final long currentTime = System.currentTimeMillis();
    return this.userApiService.getOwningTokens(uid).stream()
        .filter(userApi -> demoMode.isEnabled() || !UserApi.isExpired(userApi, currentTime))
        .toList();
  }
}
