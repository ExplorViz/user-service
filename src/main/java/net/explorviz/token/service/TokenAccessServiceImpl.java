package net.explorviz.token.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import java.util.List;
import net.explorviz.token.model.LandscapeToken;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementation of {@link TokenAccessService}.
 *
 * @see net.explorviz.token.service.TokenAccessService
 */
@ApplicationScoped
public class TokenAccessServiceImpl implements TokenAccessService {

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "false")
  /* default */ Instance<Boolean> authEnabled;

  @Override
  public TokenPermission[] getPermissions(final LandscapeToken token, final String userId) {

    if (!this.authEnabled.get()) {
      return new TokenPermission[] {
        TokenPermission.DELETE, TokenPermission.READ, TokenPermission.UPDATE
      };
    }

    if (userId == null) {
      return new TokenPermission[] {};
    }

    if (userId.equals(token.getOwnerId())) {
      return new TokenPermission[] {
        TokenPermission.READ, TokenPermission.DELETE, TokenPermission.UPDATE
      };
    }

    final List<String> sharedUsers = token.getSharedUsersIds();
    if (sharedUsers != null && sharedUsers.contains(userId)) {
      return new TokenPermission[] {TokenPermission.READ};
    }

    return new TokenPermission[] {};
  }
}
