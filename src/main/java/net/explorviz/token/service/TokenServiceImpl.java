package net.explorviz.token.service;

import java.util.Collection;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.TokenEvent;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

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
    eventService.dispatch(new TokenEvent(EventType.CREATED, token.getValue(), token.getOwnerId()));
    return token;
  }

  @Override
  public Optional<LandscapeToken> getByValue(final String tokenValue) {
    return repository.find("value = ?1", tokenValue).stream().findFirst();
  }

  @Override
  public Collection<LandscapeToken> getOwningTokens(final String ownerId) {
    return repository.findForUser(ownerId);
  }

  @Override
  public void deleteByValue(final LandscapeToken token) {
    long docsAffected = repository.delete("value = ?1", token.getValue());
    if (docsAffected == 1) {
      eventService.dispatch(new TokenEvent(EventType.DELETED, token.getValue(), token.getOwnerId()));
    }
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {

  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {

  }
}
