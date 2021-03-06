package net.explorviz.token.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

  @Inject // NOPMD
  /* default */ SecurityIdentity securityIdentity; // NOCS

  private final TokenService tokenService;

  private final TokenAccessService tokenAccessService;



  @Inject
  public TokenResource(final TokenService tokenService,
      final TokenAccessService tokenAccessService,
      final SecurityIdentity securityIdentity) {
    this.securityIdentity = securityIdentity;
    this.tokenAccessService = tokenAccessService;
    this.tokenService = tokenService;
  }


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
        this.tokenService.cloneToken(tokenId, userId, token.get().getAlias());
      }
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
