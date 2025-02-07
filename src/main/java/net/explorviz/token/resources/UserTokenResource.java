package net.explorviz.token.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
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
   * Endpoint to generate a token.
   *
   * @param userId Id of the user who owns the generated token.
   * @param alias  User-defined and optional alias for token identification.
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
      return this.tokenService.createNewToken(userId, alias.alias, alias.isRequestedFromVSCodeExtension);
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
          t.getOwnerId(), t.getCreated(), t.getAlias(), t.getIsRequestedFromVSCodeExtension()));
    }
    return cleanedTokens;
  }


  /**
   * Helper class for retrieving aliases a body data.
   */
  private static class TokenAlias {

    private final String alias;
    private final boolean isRequestedFromVSCodeExtension;

    @JsonCreator
    public TokenAlias(final String alias, final boolean isRequestedFromVSCodeExtension) {
      this.alias = alias;
      this.isRequestedFromVSCodeExtension = isRequestedFromVSCodeExtension;
    }

    public String getAlias() {
      return this.alias;
    }

    public boolean getIsRequestedFromVSCodeExtension() {
      return this.isRequestedFromVSCodeExtension;
    }
  }

}

