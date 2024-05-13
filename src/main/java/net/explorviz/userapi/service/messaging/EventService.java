package net.explorviz.userapi.service.messaging;

import net.explorviz.avro.UserAPIEvent;

/**
 * Interface for the emit event service.
 */
public interface EventService {

  void dispatch(UserAPIEvent event);

}
