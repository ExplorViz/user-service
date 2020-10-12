package net.explorviz.token.service.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.explorviz.token.model.LandscapeToken;

/**
 * Represents an event to a token.
 */
public class TokenEvent {

  /**
   * Event types.
   */
  public enum Type {
    /**
     * New token was created for a user
     */
    CREATED
  }


  private Type type;
  private LandscapeToken token;

  /**
   * Constructs a new event to be dispatched.
   * @param type the type of the event
   * @param token the subject of the event
   */
  public TokenEvent(final Type type, final LandscapeToken token) {
    this.type = type;
    this.token = token;
  }


  @JsonProperty("type")
  public Type getType() {
    return type;
  }

  @JsonProperty("token")
  public LandscapeToken getToken() {
    return token;
  }
}
