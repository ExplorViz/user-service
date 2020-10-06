package net.explorviz.token.service;

import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class UseCasesImpl implements UseCases {

  private final TokenGenerator generator;
  private final LandscapeTokenRepository repository;

  @Inject
  public UseCasesImpl(final TokenGenerator generator,
                      final LandscapeTokenRepository repository) {
    this.generator = generator;
    this.repository = repository;
  }

  @Override
  public LandscapeToken createNewToken(final String ownerId) {
    LandscapeToken token = generator.generateToken(ownerId);
    repository.persist(token);
    return token;
  }

  @Override
  public Collection<LandscapeToken> getOwningTokens(final String ownerId) {
    return repository.findForUser(ownerId);
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {

  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {

  }
}
