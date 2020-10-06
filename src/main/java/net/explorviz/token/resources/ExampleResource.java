package net.explorviz.token.resources;

import io.quarkus.security.identity.SecurityIdentity;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/hello")
@RequestScoped
public class ExampleResource {

/*
  @Inject
  JsonWebToken token;

  @Inject
  SecurityIdentity securityIdentity;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    StringBuilder builder = new StringBuilder();
    builder.append("Jwt: ").append(token.getExpirationTime()).append("\n");
    builder.append("Name: ").append(token.getSubject()).append("\n");
    builder.append("Name: ").append(token.getSubject()).append("\n");
    return builder.toString();
  }
 */

}
