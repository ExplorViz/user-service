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

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "value = ?1";

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
  public LandscapeToken createNewToken(final String ownerId, final String alias) {
    final LandscapeToken token = this.generator.generateToken(ownerId, alias);
    this.repository.persist(token);
    this.eventService
        .dispatch(new TokenEvent(EventType.CREATED, token.toAvro()));
    return token;
  }

  @Override
  public Optional<LandscapeToken> getByValue(final String tokenValue) {
    return this.repository.find(DELETE_FLAG_QUERY, tokenValue).stream().findFirst();
  }

  @Override
  public Collection<LandscapeToken> getOwningTokens(final String ownerId) {
    return this.repository.findForUser(ownerId);
  }

  @Override
  public void deleteByValue(final LandscapeToken token) {
    final long docsAffected = this.repository.delete(DELETE_FLAG_QUERY, token.getValue());
    if (docsAffected == DELETE_FLAG) {
      this.eventService
          .dispatch(new TokenEvent(EventType.DELETED, token.toAvro()));
    }
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {
    // Not implemented
  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {
    // Not implemented
  }
}
