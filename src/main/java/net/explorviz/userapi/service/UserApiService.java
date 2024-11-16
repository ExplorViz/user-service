package net.explorviz.userapi.service;

import java.util.Collection;
import net.explorviz.userapi.model.UserApi;

/**
 * Interface to manage {@link UserApi}s.
 */
public interface UserApiService {

  /**
   * Retrieve all user API tokens owned by a given user.
   *
   * @param uid the id of user
   * @return collection of all user API tokens  owned by given user
   */
  Collection<UserApi> getOwningTokens(String uid);

  /**
   * Delete a user API token.
   *
   * @param uid the user API token to delete.
   */
  int deleteByValue(String uid, String token);

  /**
   * Checks if a given token exists for given uid.
   *
   * @param uid the user API token id
   * @param token the token itself
   * @return whether a token exists
   */
  boolean tokenExists(String uid, String token);

  /**
   * Create a new user API token.
   *
   * @param uid the user to create the user API token for.
   * @param name the name of the API token.
   * @param token the token with its creation date and expiration date.
   * @param createdAt the date of the creation.
   * @param expires the date on which the token expires. Can be null.
   *
   * @return a new user
   */
  UserApi createNewUserApi(final String uid, final String name, final String token,
      final String hostUrl, Long createdAt, Long expires);
}
