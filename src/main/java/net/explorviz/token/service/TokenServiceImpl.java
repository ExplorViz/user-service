package net.explorviz.token.service;

import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.TokenEvent;

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

  private final TokenGenerator generator;
  private final LandscapeTokenRepository repository;
  private final EventService eventService;

  @Inject
  public TokenServiceImpl(final TokenGenerator generator,
                          final LandscapeTokenRepository repository,
                          final EventService eventService) {
    this.generator = generator;
    this.repository = repository;
    this.eventService = eventService;
  }

  @Override
  public LandscapeToken createNewToken(final String ownerId) {
    LandscapeToken token = generator.generateToken(ownerId);
    repository.persist(token);
    eventService.dispatch(new TokenEvent(TokenEvent.Type.CREATED, token));
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
