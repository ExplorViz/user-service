package net.explorviz.token.service.messaging;

import net.explorviz.avro.TokenEvent;

/**
 * Interface for the emit event service.
 */
public interface EventService {

  void dispatch(TokenEvent event);

}
