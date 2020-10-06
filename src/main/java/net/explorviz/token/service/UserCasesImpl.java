package net.explorviz.token.service;

import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class UserCasesImpl implements UseCases {

  private TokenGenerator generator;

  @Inject
  public UserCasesImpl(final TokenGenerator generator) {
    this.generator = generator;
  }

  @Override
  public LandscapeToken createNewToken(final String ownerId) {
    return generator.generateToken(ownerId);
  }

  @Override
  public Collection<LandscapeToken> getOwningTokens(final String ownerId) {
    return null;
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {

  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {

  }
}
