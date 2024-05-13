package net.explorviz.userapi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import net.explorviz.userapi.model.UserAPI;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementation of {@link UserAPIAccessService}.
 *
 * @see UserAPIAccessService
 */
@ApplicationScoped
public class UserAPIAccessServiceImpl implements UserAPIAccessService {

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  Instance<Boolean> authEnabled;

  @Override
  public UserAPIPermission[] getPermissions(final UserAPI userAPI, final String uId) {

    if (!this.authEnabled.get()) {
      return new UserAPIPermission[] {UserAPIPermission.DELETE, UserAPIPermission.READ};
    }

    if (userAPI.getuId().equals(uId)) {
      return new UserAPIPermission[] {UserAPIPermission.READ, UserAPIPermission.DELETE};
    }

    return new UserAPIPermission[] {};
  }
}
