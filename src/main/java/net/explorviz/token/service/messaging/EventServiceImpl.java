package net.explorviz.token.service.messaging;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import net.explorviz.telemetry.KafkaRecordSpanProcessor;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
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
  public void dispatch(final TokenEvent event) {
    final String key = event.getToken().getId();
    // Tombstone record (record with null value) indicates deletion.
    final byte[] payload =
        event.getType().equals(EventType.EVENT_TYPE_DELETED) ? null : event.toByteArray();
    this.eventEmitter.send(
        KafkaRecord.of(key, payload)
            .addMetadata(
                KafkaRecordSpanProcessor.metadataFor(
                    key, payload, payload == null ? null : event.toString())));

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sent new event {}", event);
    }
  }
}
