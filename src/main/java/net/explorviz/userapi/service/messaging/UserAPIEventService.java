package net.explorviz.userapi.service.messaging;

import net.explorviz.avro.UserAPIEvent;

/**
 * Interface for the emit event service.
 */
public interface UserAPIEventService {

  void dispatch(UserAPIEvent event);

}
