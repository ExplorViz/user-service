package net.explorviz.token.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.service.TokenService;

/**
 * HTTP endpoint to get and create {@link LandscapeToken}s for a user.
 */
@Path("user/{uid}/token")
@ApplicationScoped
public class UserTokenResource {

  private final TokenService tokenService;

  @Inject
  public UserTokenResource(final TokenService tokenService) {
    this.tokenService = tokenService;
  }

  /**
   * Helper class for retrieving aliases a body data.
   */
  private static class TokenAlias {
    private final String alias;

    @JsonCreator
    public TokenAlias(final String alias) {
      this.alias = alias;
    }

    public String getAlias() {
      return alias;
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public LandscapeToken generateToken(@PathParam("uid") final String userId,
                                      final TokenAlias alias) {
    if (alias == null || alias.alias.isBlank()) {
      return this.tokenService.createNewToken(userId);
    } else {
      return this.tokenService.createNewToken(userId, alias.alias);
    }
  }



  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<LandscapeToken> getToken(@PathParam("uid") final String userId) {
    return this.tokenService.getOwningTokens(userId);
  }

}

