package net.explorviz.token.persistence;

import java.util.List;
import net.explorviz.token.model.LandscapeToken;

/**
 * A token whose pending events are published by the holder of this object.
 *
 * @param id database id of the token
 * @param token current state of the token
 * @param events events to be published, in order of occurrence
 */
public record PendingToken(Object id, LandscapeToken token, List<PendingEvent> events) {}
