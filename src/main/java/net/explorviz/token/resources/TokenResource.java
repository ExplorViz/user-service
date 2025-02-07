package net.explorviz.token.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP endpoint to get and delete {@link LandscapeToken}s.
 */
@Path("token/{tid}")
@ApplicationScoped
public class TokenResource {


  private static final Logger LOGGER = LoggerFactory.getLogger(TokenResource.class);
  private final TokenService tokenService;
  private final TokenAccessService tokenAccessService;
  @Inject
  // NOPMD
  /* default */ SecurityIdentity securityIdentity; // NOCS

  /**
   * Resource for a software landscape token.
   *
   * @param tokenService       Service to manage software landscape tokens.
   * @param tokenAccessService Service which checks token permissions.
   * @param securityIdentity   Quarkus service for user access rights management.
   */
  @Inject
  public TokenResource(final TokenService tokenService, final TokenAccessService tokenAccessService,
      final SecurityIdentity securityIdentity) {
    this.tokenService = tokenService;
    this.tokenAccessService = tokenAccessService;
    this.securityIdentity = securityIdentity;
  }


  /**
   * Endpoint to get a previouly created token by its value.
   *
   * @param tokenVal Value of the requested landscape token.
   * @return Landscape token with requested value.
   * @throws ForbiddenException when no token with requested value is available.
   */
  @GET
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeToken getTokenByValue(@PathParam("tid") final String tokenVal) {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Trying to find token with value {}", tokenVal);
    }
    final LandscapeToken token =
        this.tokenService.getByValue(tokenVal).orElseThrow(NotFoundException::new);

    if (this.tokenAccessService.canRead(token, this.securityIdentity.getPrincipal().getName())) {
      return token;
    } else {
      throw new ForbiddenException();
    }
  }

  /**
   * Endpoint to delete a token by its Id.
   *
   * @param tokenVal Value of the token which shall be deleted.
   * @return Response with empty content.
   * @throws ForbiddenException when priviliges of user are insufficient for token deletion.
   */
  @DELETE
  @Authenticated
  @Produces(MediaType.TEXT_PLAIN)
  public Response deleteToken(@PathParam("tid") final String tokenVal) {

    final LandscapeToken token =
        this.tokenService.getByValue(tokenVal).orElseThrow(NotFoundException::new);
    final String uid = this.securityIdentity.getPrincipal().getName();
    if (this.tokenAccessService.canDelete(token, uid)) {
      this.tokenService.deleteByValue(token);
      return Response.noContent().build();
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Denied deletion-access for user {} to token with owner {}", uid,
            token.getOwnerId());
      }
      throw new ForbiddenException();
    }

  }

  /**
   * Endpoint to modify an access token, i.e. grant, remove, or clone access to it.
   *
   * @param tokenId Id of the access token.
   * @param userId  Id of the user whose access rights are modified.
   * @param method  Ei her "grant", "revoke", or "clone". Determines modification of access rights.
   * @return Response indicating whether or not the token could be found.
   */
  @Path("/{uid}")
  @POST
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifyAccessToToken(@PathParam("tid") final String tokenId,
      @PathParam("uid") final String userId, @QueryParam("method") final String method) {
    final Optional<LandscapeToken> token = this.tokenService.getByValue(tokenId);
    if (token.isPresent()) {
      if ("revoke".equals(method)) {
        this.tokenService.revokeAccess(token.get(), userId);
      } else if ("grant".equals(method)) {
        this.tokenService.grantAccess(token.get(), userId);
      } else if ("clone".equals(method)) {
        this.tokenService.cloneToken(tokenId, userId, token.get().getAlias(), token.get().getIsRequestedFromVSCodeExtension());
      }
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
