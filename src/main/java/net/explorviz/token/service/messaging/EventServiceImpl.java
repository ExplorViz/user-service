package net.explorviz.token.service.messaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.avro.TokenEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;


@ApplicationScoped
public
class EventServiceImpl implements EventService {

  private KafkaProducer<String, String> producer;
  private String eventTopic;


  @Channel("token-events")
  @Inject
  Emitter<TokenEvent> eventEmitter;


  @Override
  public void dispatch(final TokenEvent event) {
    eventEmitter.send(event);
  }

}
