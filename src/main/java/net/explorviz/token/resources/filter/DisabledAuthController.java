package net.explorviz.token.resources.filter;

import io.quarkus.security.spi.runtime.AuthorizationController;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller to disable authentication if config property {@code quarkus.oidc.enabled} is set to
 * {@code false}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class DisabledAuthController extends AuthorizationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(DisabledAuthController.class);

  @ConfigProperty(name = "authentication.enabled", defaultValue = "true") // NOPMD
  /* default */ Instance<Boolean> authEnabled; // NOCS

  @Override
  public boolean isAuthorizationEnabled() {

    if (LOGGER.isWarnEnabled() && !this.authEnabled.get()) {
      LOGGER.warn("Authentication is disabled, skipping token check");
    }

    return this.authEnabled.get();
  }
}
