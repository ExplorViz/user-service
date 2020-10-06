package net.explorviz.token.resources;

import java.awt.*;
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
import net.explorviz.token.service.UseCases;

@Path("/{uid}/token")
@RequestScoped
public class TokenResource {

  private UseCases useCases;

  @Inject
  public TokenResource(final UseCases useCases) {
    this.useCases = useCases;
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeToken generateToken(@PathParam("uid") String userId) {
    return useCases.createNewToken(userId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<LandscapeToken> getToken(@PathParam("uid") String userId) {
    return useCases.getOwningTokens(userId);
  }
}

