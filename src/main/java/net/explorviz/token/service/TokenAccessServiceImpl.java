package net.explorviz.token.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import net.explorviz.token.model.LandscapeToken;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementation of {@link TokenAccessService}.
 *
 * @see net.explorviz.token.service.TokenAccessService
 */
@ApplicationScoped
public class TokenAccessServiceImpl implements TokenAccessService {

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  // NOPMD
  /* default */ Instance<Boolean> authEnabled; // NOCS

  @Override
  public TokenPermission[] getPermissions(final LandscapeToken token, final String userId) {

    if (!this.authEnabled.get()) {
      return new TokenPermission[] {TokenPermission.DELETE, TokenPermission.READ};
    }

    if (token.getOwnerId().equals(userId)) {
      return new TokenPermission[] {TokenPermission.READ, TokenPermission.DELETE};
    }

    return new TokenPermission[] {};
  }

}
