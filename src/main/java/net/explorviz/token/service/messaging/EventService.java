package net.explorviz.token.service.messaging;

import net.explorviz.avro.TokenEvent;

public interface EventService {

  void dispatch(TokenEvent event);

}
