package net.explorviz.token.service.messaging;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.TokenEvent;
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
  /* default */ Emitter<TokenEvent> eventEmitter; // NOCS

  @Override
  public void dispatch(final TokenEvent event) {
    if (event.getType().equals(EventType.DELETED)) {
      // tombstone record
      this.eventEmitter.send(KafkaRecord.of(event.getToken().getValue(), null));
    } else {
      this.eventEmitter.send(KafkaRecord.of(event.getToken().getValue(), event));
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sent new event {}", event);
    }
  }

}
