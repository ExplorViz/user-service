package net.explorviz.token.resources;

import io.quarkus.security.Authenticated;
import java.util.Collection;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.resources.filter.ResourceOwnership;
import net.explorviz.token.service.TokenService;

/**
 * HTTP endpoint to get and create {@link LandscapeToken}s for a user.
 */
@Path("user/{uid}/token")
@RequestScoped
public class UserTokenResource {

  private static final String UID_PARAM = "uid";
  private final TokenService tokenService;


  @Inject
  public UserTokenResource(final TokenService tokenService) {
    this.tokenService = tokenService;
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @ResourceOwnership(uidField = UID_PARAM)
  public LandscapeToken generateToken(@PathParam(UID_PARAM) final String userId) {
    return this.tokenService.createNewToken(userId);
  }



  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @ResourceOwnership(uidField = UID_PARAM)
  public Collection<LandscapeToken> getToken(@PathParam(UID_PARAM) final String userId) {
    return this.tokenService.getOwningTokens(userId);
  }

}

