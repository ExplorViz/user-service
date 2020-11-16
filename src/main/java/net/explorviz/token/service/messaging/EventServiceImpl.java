package net.explorviz.token.service.messaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.avro.TokenEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public
class EventServiceImpl implements EventService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

  @Channel("token-events")
  @Inject
  Emitter<TokenEvent> eventEmitter;


  @Override
  public void dispatch(final TokenEvent event) {
    eventEmitter.send(event);
    LOGGER.info("Sent new event {}", event);
  }

}
