package net.explorviz.userapi.service;

import net.explorviz.userapi.model.UserAPI;
import org.jose4j.json.internal.json_simple.JSONObject;
import java.util.Collection;

/**
 * Interface to manage {@link UserAPI}s.
 */
public interface UserAPIService {

  /**
   * Retrieve all user API tokens owned by a given user.
   *
   * @param uId the id of user
   * @return collection of all user API tokens  owned by given user
   */
  Collection<UserAPI> getOwningTokens(String uId);

  /**
   * Delete a user API token.
   *
   * @param userAPI the user API token to delete.
   */
  int deleteByValue(String uId, String token);

  /**
   * Create a new user API token.
   *
   * @param uId the user to create the user API token for.
   * @param name the name of the API token.
   * @param token the token with its creation date and expiration date.
   * @param createdAt the date of the creation.
   * @param expires the date on which the token expires. Can be null.
   *
   * @return a new user
   */
  UserAPI createNewUserAPI(final String uId, final String name, final String token,
      Long createdAt, Long expires);

}
