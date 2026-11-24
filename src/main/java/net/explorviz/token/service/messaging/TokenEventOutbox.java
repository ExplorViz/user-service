package net.explorviz.token.service.messaging;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.explorviz.demo.DemoMode;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.persistence.PendingEvent;
import net.explorviz.token.persistence.PendingToken;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes pending events of tokens. Events of the same token are published in order and at least
 * once, i.e. consumers may receive an event repeatedly. Failed events are retried until they are
 * published.
 */
@ApplicationScoped
public class TokenEventOutbox {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenEventOutbox.class);

  // Exceeds the delivery timeout of the Kafka producer plus the time it may block before sending
  private static final long PUBLISH_TIMEOUT_MS = 40_000;
  private static final long LEASE_MS = PUBLISH_TIMEOUT_MS + 20_000;
  private static final long RETRY_DELAY_MS = 5_000;

  private final LandscapeTokenRepository tokens;
  private final SnapshotRepository snapshots;
  private final EventService eventService;
  private final ManagedExecutor executor;
  private final DemoMode demoMode;

  /**
   * Creates the outbox.
   *
   * @param tokens repository which stores pending events
   * @param snapshots repository of snapshots, which are deleted with their token
   * @param eventService service to publish events
   * @param executor executor to publish events in the background
   * @param demoMode events are not published in demo mode
   */
  @Inject
  public TokenEventOutbox(
      final LandscapeTokenRepository tokens,
      final SnapshotRepository snapshots,
      final EventService eventService,
      final ManagedExecutor executor,
      final DemoMode demoMode) {
    this.tokens = tokens;
    this.snapshots = snapshots;
    this.eventService = eventService;
    this.executor = executor;
    this.demoMode = demoMode;
  }

  /**
   * Publishes pending events of a token in the background.
   *
   * @param tokenValue the value of the token
   */
  public void publishSoon(final String tokenValue) {
    try {
      this.executor.execute(
          () -> {
            try {
              this.publishNext(tokenValue);
            } catch (final RuntimeException e) {
              LOGGER.warn("Publishing token events failed, trying again later", e);
            }
          });
    } catch (final RejectedExecutionException e) {
      LOGGER.warn("Token events could not be published immediately, trying again later", e);
    }
  }

  /** Publishes pending events of all tokens, stops on failure. */
  @Scheduled(every = "${explorviz.outbox.interval}", concurrentExecution = ConcurrentExecution.SKIP)
  public void publishAll() {
    if (this.demoMode.isEnabled()) {
      return;
    }
    while (this.publishNext(null)) {
      LOGGER.debug("Published pending events of a token");
    }
  }

  /**
   * Publishes pending events of one token.
   *
   * @return whether there was a token with pending events and all of them were published
   */
  private boolean publishNext(final String tokenValue) {
    Object id = null;
    try {
      final long now = System.currentTimeMillis();
      Optional<PendingToken> pending = this.tokens.claimPending(tokenValue, now, now + LEASE_MS);
      if (pending.isEmpty()) {
        return false;
      }
      id = pending.get().id();
      while (pending.isPresent() && this.publishFirst(pending.get())) {
        pending = this.tokens.extendLease(id, System.currentTimeMillis() + LEASE_MS);
      }
      return true;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | TimeoutException e) {
      LOGGER.warn("Publishing a token event failed, trying again later", e);
      this.tokens.extendLease(id, System.currentTimeMillis() + RETRY_DELAY_MS);
    }
    return false;
  }

  /** Returns whether there may be further events. */
  private boolean publishFirst(final PendingToken pending)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (pending.events().isEmpty()) {
      return !this.tokens.finishPublishing(pending.id());
    }
    final LandscapeToken token = pending.token();
    final PendingEvent event = pending.events().getFirst();
    final boolean isDeletion = event.type() == EventType.EVENT_TYPE_DELETED;
    if (isDeletion) {
      this.snapshots.deleteForToken(token.getOwnerId(), token.getValue());
    }

    this.eventService
        .dispatch(toTokenEvent(token, event))
        .toCompletableFuture()
        .get(PUBLISH_TIMEOUT_MS, TimeUnit.MILLISECONDS);

    if (isDeletion) {
      this.tokens.removeDeleted(pending.id());
      return false;
    }
    this.tokens.removeEvent(pending.id(), event);
    return true;
  }

  private static TokenEvent toTokenEvent(final LandscapeToken token, final PendingEvent event) {
    final TokenEvent.Builder builder =
        TokenEvent.newBuilder().setType(event.type()).setToken(token.toProtobuf());
    if (event.userId() != null) {
      builder.setUserId(event.userId());
    }
    if (event.clonedTokenId() != null) {
      builder.setClonedTokenId(event.clonedTokenId());
    }
    return builder.build();
  }
}
