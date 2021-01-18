package net.explorviz.token.resources;

import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
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
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenService;
import org.eclipse.microprofile.jwt.JsonWebToken;


@Path("user/{uid}/token")
@RequestScoped
public class UserTokenResource {

  private final TokenService tokenService;
  private final JsonWebToken jwt;
  private  final TokenAccessService tokenAccessService;

  @Inject
  public UserTokenResource(final TokenService tokenService,
                           final TokenAccessService tokenAccessService,
                           final JsonWebToken jwt) {
    this.tokenService = tokenService;
    this.jwt = jwt;
    this.tokenAccessService = tokenAccessService;
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @ResourceOwnership(uidField = "uid")
  public LandscapeToken generateToken(@PathParam("uid") String userId) {

    if (jwt.getSubject() == null || jwt.getSubject().isEmpty()) {
      throw new UnauthorizedException("Unauthorized");
    }

    if (!userId.equals(jwt.getSubject())) {
      throw new ForbiddenException("Forbidden");
    }

    return tokenService.createNewToken(userId);
  }



  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  @ResourceOwnership(uidField = "uid")
  public Collection<LandscapeToken> getToken(@PathParam("uid") String userId) {
    return tokenService.getOwningTokens(userId);
  }

}

