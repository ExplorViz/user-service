package net.explorviz.token.generator;

import net.explorviz.token.model.LandscapeToken;

/**
 * Generate new landscape tokens.
 */
public interface TokenGenerator {

  /**
   * Generates a new landscape token associated with a given user.
   * @param ownerId the id of the user the token is generated for
   * @return a new landscape token.
   */
  LandscapeToken generateToken(String ownerId);

}
