package net.explorviz.token.resources.filter;

import io.quarkus.security.spi.runtime.AuthorizationController;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller to disable authentication if config property {@code explorviz.auth.enabled} is set to
 * {@code false}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class DisabledAuthController extends AuthorizationController {

  private static Logger LOGGER = LoggerFactory.getLogger(DisabledAuthController.class);

  @ConfigProperty(name = "explorviz.auth.enabled", defaultValue = "true") // NOPMD
  /* default */ boolean authEnabled; // NOCS

  @Override
  public boolean isAuthorizationEnabled() {

    if (!this.authEnabled) {
      LOGGER.warn("Authentication is disabled");
    }

    return this.authEnabled;
  }
}
