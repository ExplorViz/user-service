package net.explorviz.token.service.messaging;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import net.explorviz.telemetry.KafkaRecordSpanProcessor;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Kafka-based emitter service to send information about {@link TokenEvent}s to other services. */
@ApplicationScoped
public class EventServiceImpl implements EventService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

  @Channel("token-events")
  @Inject
  /* default */ Emitter<byte[]> eventEmitter;

  @Override
  public CompletionStage<Void> dispatch(final TokenEvent event) {
    final String key = event.getToken().getId();
    // Tombstone record (record with null value) indicates deletion.
    final byte[] payload =
        event.getType().equals(EventType.EVENT_TYPE_DELETED) ? null : event.toByteArray();
    final CompletableFuture<Void> result = new CompletableFuture<>();
    final Message<byte[]> message =
        KafkaRecord.of(key, payload)
            .addMetadata(
                KafkaRecordSpanProcessor.metadataFor(
                    key, payload, payload == null ? null : event.toString()))
            .withAck(
                () -> {
                  result.complete(null);
                  return CompletableFuture.completedFuture(null);
                })
            .withNack(
                failure -> {
                  result.completeExceptionally(failure);
                  return CompletableFuture.completedFuture(null);
                });
    try {
      this.eventEmitter.send(message);
    } catch (final IllegalStateException e) {
      // The channel is not ready or its buffer is full
      return CompletableFuture.failedFuture(e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sending new event {}", event);
    }
    return result;
  }
}
