package net.explorviz.userapi.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Collection;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserApiServiceImpl implements UserApiService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserApiService.class);


  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "uid = ?1 and token = ?2";
  private final UserApiRepository repository;
  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  /* default */ Instance<Boolean> authEnabled; // NOCS
  @ConfigProperty(name = "initial.token.creation.enabled")
  /* default */ boolean initialTokenCreationEnabled; // NOCS
  @ConfigProperty(name = "initial.token.user")
  /* default */ String initialTokenUser; // NOCS
  @ConfigProperty(name = "initial.token.value")
  /* default */ String initialTokenValue; // NOCS
  @ConfigProperty(name = "initial.token.secret")
  /* default */ String initialTokenSecret; // NOCS
  @ConfigProperty(name = "initial.token.alias")
  /* default */ String initialTokenAlias; // NOCS

  @Inject
  public UserApiServiceImpl(UserApiRepository repository) {
    this.repository = repository;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantUserApi(this.initialTokenUser, this.initialTokenAlias,
          this.initialTokenValue, "testUrl", 0L);
      LOGGER.atDebug().log("Created default user API token.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantUserApi(final String uid, final String name, final String token,
      final String hostUrl, final Long expires) {
    final long createdAt = System.currentTimeMillis();

    final UserApi userApi =
        new UserApi(uid, name, token, hostUrl, createdAt, expires);
    this.repository.persist(userApi);
  }

  @Override
  public Collection<UserApi> getOwningTokens(final String uid) {
    return this.repository.findForUser(uid);
  }

  @Override
  public int deleteByValue(final String uid, final String token) {
    Collection<UserApi> userApiToDelete = this.repository.findForUserAndToken(uid, token);

    if (userApiToDelete.size() != 1) {
      return -1;
    }

    this.repository.delete(DELETE_FLAG_QUERY, uid, token);

    return 0;
  }

  @Override
  public boolean tokenExists(final String uid, final String token) {
    Collection<UserApi> tokenUid = this.repository.findForUserAndToken(uid, token);

    return !tokenUid.isEmpty();
  }

  @Override
  public UserApi createNewUserApi(final String uid, final String name, final String token,
      final String hostUrl, final Long createdAt, final Long expires) {
    final UserApi userApi = new UserApi(uid, name, token, hostUrl, createdAt, expires);
    this.repository.persist(userApi);

    return userApi;
  }

}
