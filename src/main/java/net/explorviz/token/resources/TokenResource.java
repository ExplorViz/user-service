package net.explorviz.token.resources;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.explorviz.token.model.LandscapeToken;
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


  @Inject
  public TokenResource(final TokenService tokenService) {
    this.tokenService = tokenService;
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeToken getTokenByValue(@PathParam("tid") final String tokenVal) {
    LOGGER.info("Trying to find token with value {}", tokenVal);
    final Optional<LandscapeToken> got = this.tokenService.getByValue(tokenVal);
    return got.orElseThrow(() -> new NotFoundException("No token with such value"));


  }

  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  public Response deleteToken(@PathParam("tid") final String tokenVal) {
    final Optional<LandscapeToken> token = this.tokenService.getByValue(tokenVal);
    if (token.isPresent()) {
      this.tokenService.deleteByValue(token.get());
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

}
