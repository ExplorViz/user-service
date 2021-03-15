package net.explorviz.token.resources.filter;

import io.quarkus.security.spi.runtime.AuthorizationController;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Controller to disable authentication if config property {@code disable.authorization} is set to
 * {@code true}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class DisabledAuthController extends AuthorizationController {

  @ConfigProperty(name = "explorviz.auth.enabled", defaultValue = "true")
  boolean authEnabled;

  @Override
  public boolean isAuthorizationEnabled() {
    return this.authEnabled;
  }
}
