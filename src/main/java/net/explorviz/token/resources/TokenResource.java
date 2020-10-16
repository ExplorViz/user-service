package net.explorviz.token.resources;

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
import net.explorviz.token.service.TokenService;

@Path("/{uid}/token")
@RequestScoped
public class TokenResource {

  private TokenService tokenService;

  @Inject
  public TokenResource(final TokenService tokenService) {
    this.tokenService = tokenService;
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeToken generateToken(@PathParam("uid") String userId) {
    return tokenService.createNewToken(userId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<LandscapeToken> getToken(@PathParam("uid") String userId) {
    return tokenService.getOwningTokens(userId);
  }

}

