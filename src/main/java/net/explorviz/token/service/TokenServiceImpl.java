package net.explorviz.token.service;

import com.mongodb.MongoWriteException;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import net.explorviz.demo.DemoMode;
import net.explorviz.persistence.DuplicateKeys;
import net.explorviz.proto.EventType;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.persistence.PendingEvent;
import net.explorviz.token.service.messaging.TokenEventOutbox;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the use cases for managing and accessing tokens. */
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

  @Inject DemoMode demoMode;

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

  private final TokenGenerator generator;
  private final LandscapeTokenRepository repository;
  private final TokenEventOutbox outbox;
  private final SnapshotRepository snapshotRepository;

  @ConfigProperty(name = "quarkus.oidc.tenant-enabled", defaultValue = "false")
  /* default */ Instance<Boolean> authEnabled;

  @ConfigProperty(name = "initial.token.creation.enabled")
  /* default */ boolean initialTokenCreationEnabled;

  @ConfigProperty(name = "initial.token.user")
  /* default */ String initialTokenUser;

  @ConfigProperty(name = "initial.token.value")
  /* default */ String initialTokenValue;

  @ConfigProperty(name = "initial.token.secret")
  /* default */ String initialTokenSecret;

  @ConfigProperty(name = "initial.token.alias")
  /* default */ String initialTokenAlias;

  /**
   * Implementation of the token service, responsible for token management.
   *
   * @param generator Object which can generate new landscape tokens.
   * @param repository MongoRepository for landscape tokens.
   * @param outbox Publishes pending events of tokens.
   * @param snapshotRepository MongoRepository for snapshots.
   */
  @Inject
  public TokenServiceImpl(
      final TokenGenerator generator,
      final LandscapeTokenRepository repository,
      final TokenEventOutbox outbox,
      final SnapshotRepository snapshotRepository) {
    this.generator = generator;
    this.repository = repository;
    this.outbox = outbox;
    this.snapshotRepository = snapshotRepository;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (demoMode.isEnabled()) {
      return;
    }
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantToken(
          this.initialTokenUser,
          this.initialTokenValue,
          this.initialTokenSecret,
          this.initialTokenAlias);
      LOGGER.atDebug().log("Created default landscape token.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantToken(
      final String ownerId,
      final String value,
      final String secret,
      final String initialTokenAlias) {
    // Avoid creation of duplicate tokens
    if (this.repository.isValueInUse(value)) {
      return;
    }

    final long created = System.currentTimeMillis();

    final LandscapeToken token =
        new LandscapeToken(
            value, secret, ownerId, created, initialTokenAlias, Collections.emptyList());
    try {
      this.repository.create(token, PendingEvent.of(EventType.EVENT_TYPE_CREATED));
    } catch (final MongoWriteException e) {
      if (DuplicateKeys.isDuplicateKey(e)) {
        LOGGER.info("Default landscape token was not created, its value is already in use.");
        return;
      }
      throw e;
    }
    this.outbox.publishSoon(value);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Created default landscape token.");
    }
  }

  @Override
  public LandscapeToken createNewToken(final String ownerId, final String alias) {
    return this.createNewToken(ownerId, alias, null, null);
  }

  @Override
  public LandscapeToken createNewToken(
      final String ownerId, final String alias, final String value, final String secret) {
    return this.create(
        ownerId, alias, value, secret, PendingEvent.of(EventType.EVENT_TYPE_CREATED));
  }

  @Override
  public LandscapeToken cloneToken(
      final String oldTokenId, final String newOwnerId, final String alias) {
    return this.create(
        newOwnerId,
        alias,
        null,
        null,
        PendingEvent.of(EventType.EVENT_TYPE_CREATED),
        PendingEvent.ofClone(oldTokenId));
  }

  private LandscapeToken create(
      final String ownerId,
      final String alias,
      final String value,
      final String secret,
      final PendingEvent... events) {
    final String effectiveAlias = alias == null ? "" : alias;
    if (value != null && !value.isBlank() && this.repository.isValueInUse(value.trim())) {
      throw new WebApplicationException(DuplicateKeys.conflict("Token value already exists"));
    }

    final LandscapeToken token =
        this.generator.generateToken(ownerId, effectiveAlias, value, secret);
    this.repository.create(token, events);
    this.outbox.publishSoon(token.getValue());
    return token;
  }

  @Override
  public Optional<LandscapeToken> getByValue(final String tokenValue) {
    return this.repository.findByValue(tokenValue);
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
  public Collection<LandscapeToken> getAllTokens() {
    return this.repository.getAll();
  }

  @Override
  public void deleteByValue(final LandscapeToken token) {
    if (this.repository.markDeleted(token.getValue())) {
      // The outbox repeats this in case the service stops right now
      this.snapshotRepository.deleteForToken(token.getOwnerId(), token.getValue());
      this.outbox.publishSoon(token.getValue());
    }
  }

  @Override
  public void grantAccess(final LandscapeToken token, final String userId) {
    if (this.repository.grantAccess(token.getValue(), userId)) {
      this.outbox.publishSoon(token.getValue());
    }
  }

  @Override
  public void revokeAccess(final LandscapeToken token, final String userId) {
    if (this.repository.revokeAccess(token.getValue(), userId)) {
      this.outbox.publishSoon(token.getValue());
    }
  }

  @Override
  public void updateAlias(final LandscapeToken token, final String newAlias) {
    if (this.repository.updateAlias(token.getValue(), newAlias == null ? "" : newAlias)) {
      this.outbox.publishSoon(token.getValue());
    }
  }
}
