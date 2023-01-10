package net.explorviz.token.generator;

import net.explorviz.token.model.LandscapeToken;

/**
 * Generate new landscape tokens.
 */
public interface TokenGenerator {

  /**
   * Generates a new landscape token associated with a given user.
   *
   * @param ownerId the id of the user the token is generated for
   * @return a new landscape token.
   */
  default LandscapeToken generateToken(final String ownerId) {
    return this.generateToken(ownerId, "");
  }

  /**
   * Generates a new landscape token associated with a given user. Assigns an alias to this token.
   *
   * @param ownerId the id of the user the token is generated for
   * @param alias   the alias for the token
   * @return a new landscape token.
   */
  LandscapeToken generateToken(String ownerId, String alias);

}
