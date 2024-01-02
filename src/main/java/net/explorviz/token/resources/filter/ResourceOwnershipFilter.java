package net.explorviz.token.resources.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether the calling user is the owner of the accessed resource. If not, a "403 -
 * Forbidden" is returned.
 */
@Priority(Priorities.AUTHORIZATION)
@Provider
@ResourceOwnership
public class ResourceOwnershipFilter implements ContainerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOwnershipFilter.class);

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  // NOPMD
  /* default */ Instance<Boolean> authEnabled; // NOCS

  @Context
  // NOPMD
  /* default */ ResourceInfo resourceInfo; // NOCS

  @Context
  // NOPMD
  /* default */ UriInfo uriInfo; // NOCS


  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (!this.authEnabled.get()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Authorization is disabled, skipping ownership check");
      }
      return;
    }

    final Principal p = requestContext.getSecurityContext().getUserPrincipal();

    // Somehow Quarkus executes this filter before RoleAllowed are checked.
    // In this case, return 401 if no user principals are given in the request
    if (p == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Restricted route accessed anonymously, aborting request");
      }
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build());
      return;
    }

    final String uidParam =
        this.resourceInfo.getResourceMethod().getAnnotation(ResourceOwnership.class).uidField();

    final String uid = this.uriInfo.getPathParameters().get(uidParam).get(0);

    if (p.getName() == null || !p.getName().equals(uid)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Denied access for user {} (owner: {})", p.getName(), uid);
      }
      throw new ForbiddenException();
    }

  }
}
