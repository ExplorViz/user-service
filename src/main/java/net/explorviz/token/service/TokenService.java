package net.explorviz.token.service;

import java.util.Collection;
import net.explorviz.token.model.LandscapeToken;

public interface TokenService {


  /**
   * Retrieve all tokens owned by a given user.
   *
   * @param ownerId the id of user
   * @return collection of all tokens owned by given user
   */
  Collection<LandscapeToken> getOwningTokens(String ownerId);

  /**
   * Delete a token.
   *
   * @param token the token to delete.
   */
  void deleteToken(LandscapeToken token);

  /**
   * Create a new token for a given user.
   *
   * @param ownerId the user to create the token for
   * @return a new token
   */
  LandscapeToken createNewToken(String ownerId);

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

}
