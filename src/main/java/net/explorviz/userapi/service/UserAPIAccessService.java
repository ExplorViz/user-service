package net.explorviz.userapi.service;

import net.explorviz.userapi.model.UserAPI;
import java.util.Arrays;

/**
 * Service for checking user permissions.
 */
public interface UserAPIAccessService {

  UserAPIPermission[] getPermissions(UserAPI userAPI, String uId);

  /**
   * Checks whether a user can read a user resource.
   *
   * @param userAPI  the user with the API token
   * @param uId the id of the user
   * @return {@code true} iff read access is granted to the user
   */
  default boolean canRead(final UserAPI userAPI, final String uId) {
    return Arrays.asList(this.getPermissions(userAPI, uId)).contains(UserAPIPermission.READ);
  }

  /**
   * Checks whether a user can delete a user api.
   *
   * @param userAPI  the user with the API token
   * @param uId the id of the user
   * @return {@code true} iff the user is allowed deleteing the user api
   */
  default boolean canDelete(final UserAPI userAPI, final String uId) {
    return Arrays.asList(this.getPermissions(userAPI, uId)).contains(UserAPIPermission.DELETE);
  }

}
