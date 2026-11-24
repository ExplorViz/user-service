package net.explorviz.token.service.messaging;

import java.util.concurrent.CompletionStage;
import net.explorviz.proto.TokenEvent;

/** Interface for the emit event service. */
public interface EventService {

  /**
   * Sends an event.
   *
   * @param event the event
   * @return completes when the event was sent, or exceptionally if this failed
   */
  CompletionStage<Void> dispatch(TokenEvent event);
}
