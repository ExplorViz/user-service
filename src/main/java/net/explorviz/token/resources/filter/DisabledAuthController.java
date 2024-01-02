package net.explorviz.token.resources.filter;

import io.quarkus.security.spi.runtime.AuthorizationController;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.interceptor.Interceptor;
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

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  /* default */ Instance<Boolean> authEnabled; // NOCS

  @Override
  public boolean isAuthorizationEnabled() {

    if (LOGGER.isWarnEnabled() && !this.authEnabled.get()) {
      LOGGER.warn("Authentication is disabled, skipping token check");
    }

    return this.authEnabled.get();
  }
}
