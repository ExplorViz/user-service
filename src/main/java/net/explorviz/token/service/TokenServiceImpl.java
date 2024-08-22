package net.explorviz.token.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import net.explorviz.avro.EventType;
import net.explorviz.avro.TokenEvent;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the use cases for managing and accessing tokens.
 */
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "value = ?1";
  private final TokenGenerator generator;
  private final LandscapeTokenRepository repository;
  private final EventService eventService;
  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  /* default */ Instance<Boolean> authEnabled; // NOCS
  @ConfigProperty(name = "initial.token.creation.enabled")
  /* default */ boolean initialTokenCreationEnabled; // NOCS
  @ConfigProperty(name = "initial.token.user")
  /* default */ String initialTokenUser; // NOCS
  @ConfigProperty(name = "initial.token.value")
  /* default */ String initialTokenValue; // NOCS
  @ConfigProperty(name = "initial.token.secret")
  /* default */ String initialTokenSecret; // NOCS
  @ConfigProperty(name = "initial.token.alias")
  /* default */ String initialTokenAlias; // NOCS

  /**
   * Implementation of the token service, responsible for token management.
   *
   * @param generator    Object which can generate new landscape tokens.
   * @param repository   MongoRepository for landscape tokens.
   * @param eventService Service to emit events.
   */
  @Inject
  public TokenServiceImpl(final TokenGenerator generator, final LandscapeTokenRepository repository,
      final EventService eventService) {
    this.generator = generator;
    this.repository = repository;
    this.eventService = eventService;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (this.initialTokenCreationEnabled) {
      for (int i = 1; i <= 10; i++) {
        this.createNewConstantToken(this.initialTokenUser,
            this.initialTokenValue + Integer.toString(i),
            this.initialTokenSecret + Integer.toString(i), "Landscape " + Integer.toString(i));
        LOGGER.atDebug().log("Created default landscape token.");
      }
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantToken(final String ownerId, final String value, final String secret,
      final String initialTokenAlias) {
    // Avoid creation of duplicate tokens
    if (this.getByValue(value).isPresent()) {
      return;
    }

    final long created = System.currentTimeMillis();

    final LandscapeToken token =
        new LandscapeToken(value, secret, ownerId, created, initialTokenAlias,
            Collections.emptyList());
    this.repository.persist(token);
    this.eventService.dispatch(new TokenEvent(EventType.CREATED, token.toAvro(), ""));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Created default landscape token.");
    }
  }

  @Override
  public LandscapeToken createNewToken(final String ownerId, final String alias) {
    final LandscapeToken token = this.generator.generateToken(ownerId, alias);
    this.repository.persist(token);
    this.eventService.dispatch(new TokenEvent(EventType.CREATED, token.toAvro(), ""));
    return token;
  }

  @Override
  public LandscapeToken cloneToken(final String oldTokenId, final String newOwnerId,
      final String alias) {
    final var token = this.createNewToken(newOwnerId, alias);
    this.eventService.dispatch(new TokenEvent(EventType.CLONED, token.toAvro(), oldTokenId));
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
  public Collection<LandscapeToken> getSharedTokens(final String userId) {
    return this.repository.findSharedForUser(userId);
  }

  @Override
  public void deleteByValue(final LandscapeToken token) {
    final long docsAffected = this.repository.delete(DELETE_FLAG_QUERY, token.getValue());
    if (docsAffected == DELETE_FLAG) {
      this.eventService.dispatch(new TokenEvent(EventType.DELETED, token.toAvro(), ""));

    }
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {
    // Document doc = new Document("$push", new Document("sharedUsers", userId));
    // this.repository.mongoCollection().updateOne(
    // Filters.eq("value", token.getValue()),
    // doc);

    // the $set is a workaround till quarkus 1.13
    // https://github.com/quarkusio/quarkus/issues/9956
    this.repository.update("{ $addToSet: { sharedUsers: ?1 } } }, $set: { ownerId: '$ownerId'}",
        userId).where(DELETE_FLAG_QUERY, token.getValue());
    // update("{ $push: { sharedUsers: ?1 } } }", userId).where(DELETE_FLAG_QUERY,
    // token.getValue());
  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {
    this.repository.update("{ $pull: { sharedUsers: ?1 } } }, $set: { ownerId: '$ownerId'}", userId)
        .where(DELETE_FLAG_QUERY, token.getValue());
  }
}
