package net.explorviz.token.service.messaging;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka-based emitter service to send information about {@link TokenEvent}s to other services.
 */
@ApplicationScoped
public class EventServiceImpl implements EventService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

  @Channel("token-events")
  @Inject
  /* default */ Emitter<byte[]> eventEmitter; // NOCS

  @Override
  public void dispatch(final TokenEvent event) {
    if (event.getType().equals(EventType.EVENT_TYPE_DELETED)) {
      // Tombstone record (record with null value) indicates deletion
      this.eventEmitter.send(KafkaRecord.of(event.getToken().getId(), null));
    } else {
      this.eventEmitter.send(KafkaRecord.of(event.getToken().getId(), event.toByteArray()));
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sent new event {}", event);
    }
  }
}
