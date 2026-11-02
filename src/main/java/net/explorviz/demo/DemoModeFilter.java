package net.explorviz.demo;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Reject mutations before resource methods can change data or emit events. */
@Provider
@Priority(Priorities.AUTHORIZATION - 1)
public class DemoModeFilter implements ContainerRequestFilter {
  @Inject DemoMode demoMode;

  @Override
  public void filter(final ContainerRequestContext request) {
    if (!demoMode.isEnabled()) {
      return;
    }
    final String method = request.getMethod();
    final boolean safeMethod =
        "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);

    if (!safeMethod) {
      request.abortWith(
          Response.status(Response.Status.FORBIDDEN)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .entity(Map.of("code", "DEMO_MODE", "message", DemoMode.MESSAGE))
              .build());
    }
  }
}
