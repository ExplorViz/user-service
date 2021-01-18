package net.explorviz.token.resources.filter;

import io.quarkus.security.spi.runtime.AuthorizationController;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;
import javax.ws.rs.core.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Controller to disable authentication if config property {@code disable.authorization} is set to
 * {@code true}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class DisabledAuthController extends AuthorizationController {

  @ConfigProperty(name = "disable.authorization", defaultValue = "false")
  boolean disableAuthorization;

  @Override
  public boolean isAuthorizationEnabled() {
    return !disableAuthorization;
  }
}
