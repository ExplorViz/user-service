package net.explorviz.token.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.quarkus.security.Authenticated;
import java.util.ArrayList;
import java.util.Collection;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
      return this.alias;
    }
  }

  /**
   * Endpoint to generate a token.
   *
   * @param userId Id of the user who owns the generated token.
   * @param alias User-defined and optional alias for token identification.
   * @return Generated landscape token.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @ResourceOwnership(uidField = UID_PARAM)
  @Consumes(MediaType.APPLICATION_JSON)
  public LandscapeToken generateToken(@PathParam("uid") final String userId,
      final TokenAlias alias) {
    if (alias == null || alias.alias.isBlank()) {
      return this.tokenService.createNewToken(userId);
    } else {
      return this.tokenService.createNewToken(userId, alias.alias);
    }

  }

  /**
   * Get all tokens associated with a specified user.
   *
   * @param userId Id of user who has access to requested tokens, i.e. owned and shared tokens.
   * @return Collection of applicable landscape tokens.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ResourceOwnership(uidField = UID_PARAM)
  @Authenticated
  public Collection<LandscapeToken> getToken(@PathParam(UID_PARAM) final String userId) {
    final Collection<LandscapeToken> tokens = this.tokenService.getOwningTokens(userId);
    final Collection<LandscapeToken> shared = this.tokenService.getSharedTokens(userId);

    tokens.addAll(this.cleanSharedTokens(shared));
    return tokens;
  }

  /**
   * Cleans a collection of shared tokens by hiding some attributes: The secret and the list of
   * other users the token was shared to.
   *
   * @param tokens tokens to clean
   * @return List of cleaned tokens.
   */
  private Collection<LandscapeToken> cleanSharedTokens(final Collection<LandscapeToken> tokens) {
    final Collection<LandscapeToken> cleanedTokens = new ArrayList<>();
    for (final LandscapeToken t : tokens) {
      cleanedTokens.add(new LandscapeToken(t.getValue(), "", // NOPMD
          t.getOwnerId(), t.getCreated(), t.getAlias()));
    }
    return cleanedTokens;
  }

}

