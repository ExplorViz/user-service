package net.explorviz.token.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import java.util.Optional;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("token/{tid}")
@ApplicationScoped
public class TokenResource {


  private static final Logger LOGGER = LoggerFactory.getLogger(TokenResource.class);

  private final TokenService tokenService;

  private final TokenAccessService tokenAccessService;

  @Inject
  SecurityIdentity securityIdentity;

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
  public LandscapeToken getTokenByValue(@PathParam("tid") String tokenVal) {

    LOGGER.info("Trying to find token with value {}", tokenVal);
    LandscapeToken token = tokenService.getByValue(tokenVal).orElseThrow(NotFoundException::new);

    if (tokenAccessService.canRead(token, securityIdentity.getPrincipal().getName())) {
      return token;
    } else {
      throw new ForbiddenException();
    }
  }

  @DELETE
  @Authenticated
  @Produces(MediaType.TEXT_PLAIN)
  public Response deleteToken(@PathParam("tid") String tokenVal) {

    LandscapeToken token = tokenService.getByValue(tokenVal).orElseThrow(NotFoundException::new);
    String uid =  securityIdentity.getPrincipal().getName();
    if (tokenAccessService.canDelete(token, uid)) {
      tokenService.deleteByValue(token);
      return Response.noContent().build();
    } else {
      LOGGER.info("Denied deletion-access for user {} to token with owner {}", uid, token.getOwnerId());
      throw new ForbiddenException();
    }

  }

}
