package net.explorviz.token.resources.filter;

import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(Priorities.AUTHENTICATION)
@Provider
@ResourceOwnership
public class ResourceOwnershipFilter implements ContainerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOwnershipFilter.class);

  @Context
  ResourceInfo resourceInfo;

  @Context
  UriInfo uriInfo;

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    Principal p = requestContext.getSecurityContext().getUserPrincipal();
    String uidParam = resourceInfo.getResourceMethod().getAnnotation(ResourceOwnership.class).uidField();

    String uid = uriInfo.getPathParameters().get(uidParam).get(0);

    if (!p.getName().equals(uid)) {
      LOGGER.info("Denied access for user {} (owner: {})", p.getName(), uid);
      throw new ForbiddenException();
    }

  }
}
