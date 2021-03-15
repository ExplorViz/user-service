package net.explorviz.token.service;

import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;

/**
 * Implementation of {@link TokenAccessService}.
 *
 * @see net.explorviz.token.service.TokenAccessService
 */
@ApplicationScoped
public class TokenAccessServiceImpl implements TokenAccessService {

  @Override
  public TokenPermission[] getPermissions(final LandscapeToken token, final String userId) {

    if (token.getOwnerId().equals(userId)) {
      return new TokenPermission[] {TokenPermission.READ, TokenPermission.DELETE};
    }

    return new TokenPermission[] {};
  }

}
