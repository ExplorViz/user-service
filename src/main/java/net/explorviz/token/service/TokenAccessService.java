package net.explorviz.token.service;

import java.util.Arrays;
import net.explorviz.token.model.LandscapeToken;

/**
 * Service for checking token permissions.
 */
public interface TokenAccessService {

  TokenPermission[] getPermissions(LandscapeToken token, String userId);

  /**
   * Checks whether a user can read a landscape token
   *
   * @param token the token
   * @param userId the id of the user
   * @return {@code true} iff read access is granted to the user
   */
  default boolean canRead(final LandscapeToken token, final String userId) {
    return Arrays.asList(this.getPermissions(token, userId)).contains(TokenPermission.READ);
  }

  /**
   * Checks whether a user can delete a landscape token
   *
   * @param token the token
   * @param userId the id of the user
   * @return {@code true} iff the user is allowed deleted the token
   */
  default boolean canDelete(final LandscapeToken token, final String userId) {
    return Arrays.asList(this.getPermissions(token, userId)).contains(TokenPermission.DELETE);
  }



}
