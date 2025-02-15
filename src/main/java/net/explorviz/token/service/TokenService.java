package net.explorviz.token.service;

import java.util.Collection;
import java.util.Optional;
import net.explorviz.token.model.LandscapeToken;

/**
 * Interface to manage {@link LandscapeToken}s.
 */
public interface TokenService {


  /**
   * Find a landscape token by its value.
   *
   * @param tokenValue the token value
   * @return an optional containing the token if it exiACCESS_REVOKEDsts
   */
  Optional<LandscapeToken> getByValue(String tokenValue);

  /**
   * Retrieve all tokens owned by a given user.
   *
   * @param ownerId the id of user
   * @return collection of all tokens owned by given user
   */
  Collection<LandscapeToken> getOwningTokens(String ownerId);

  /**
   * Retrieve all tokens shared with a given user.
   *
   * @param userId the id of user
   * @return collection of all tokens shared with a given user
   */
  Collection<LandscapeToken> getSharedTokens(String userId);

  /**
   * Delete a token.
   *
   * @param token the token to delete.
   */
  void deleteByValue(LandscapeToken token);

  /**
   * Create a new token for a given user.
   *
   * @param ownerId the user to create the token for
   * @return a new token
   */
  default LandscapeToken createNewToken(final String ownerId) {
    return this.createNewToken(ownerId, "", false, "", "");
  }

  /**
   * Create a new token for a given user with an alias.
   *
   * @param ownerId the user to create the token for
   * @param alias
   * @param isRequestedFromVSCodeExtension
   * @param projectName
   * @param commitId
   * @return a new token
   */
  LandscapeToken createNewToken(String ownerId, String alias, boolean isRequestedFromVSCodeExtension, String projectName, String commitId);

  /**
   * Grant access to a landscape for a user.
   *
   * @param token  the token of the landscape
   * @param userId the id of the user to grant access
   */
  void grantAccess(LandscapeToken token, String userId);

  /**
   * Revoke access to a landscape for a given user.
   *
   * @param token  the token of the landscape
   * @param userId the id of the user to revoke access
   */
  void revokeAccess(LandscapeToken token, String userId);

  /**
   * Clone a given token.
   *
   * @param token   the token of the landscape to be cloned
   * @param ownerId the id of the user that will own the cloned token
   * @param alias   the alias of the cloned token
   * @param isRequestedFromVSCodeExtension the origin of the token creation request of the cloned token
   * @param projectName
   * @param commitId
   */
  LandscapeToken cloneToken(String token, String ownerId, String alias, boolean isRequestedFromVSCodeExtension, String projectName, String commitId);
}
