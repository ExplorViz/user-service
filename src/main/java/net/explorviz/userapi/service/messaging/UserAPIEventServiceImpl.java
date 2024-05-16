package net.explorviz.userapi.service.messaging;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.UserAPIEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka-based emitter service to send information about {@link UserAPIEvent}s to other services.
 */
@ApplicationScoped
public class UserAPIEventServiceImpl implements UserAPIEventService {

  private static final Logger
      LOGGER = LoggerFactory.getLogger(UserAPIEventServiceImpl.class);

  @Channel("userapi-events")
  @Inject
  /* default */ Emitter<UserAPIEvent> eventEmitter;

  @Override
  public void dispatch(final UserAPIEvent event) {
    if (event.getType().equals(EventType.DELETED)) {
      // tombstone record
      this.eventEmitter.send(KafkaRecord.of(event.getUserAPI().getToken(), null));
    } else {
      this.eventEmitter.send(KafkaRecord.of(event.getUserAPI().getToken(), event));
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sent new event {}", event);
    }
  }

}
