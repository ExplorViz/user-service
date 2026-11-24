package net.explorviz.userapi.service;

import com.mongodb.MongoWriteException;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Collection;
import net.explorviz.demo.DemoMode;
import net.explorviz.persistence.DuplicateKeys;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserApiServiceImpl implements UserApiService {

  @Inject DemoMode demoMode;

  private static final Logger LOGGER = LoggerFactory.getLogger(UserApiService.class);

  private static final String DELETE_FLAG_QUERY = "uid = ?1 and token = ?2";
  private final UserApiRepository repository;

  @ConfigProperty(name = "quarkus.oidc.tenant-enabled", defaultValue = "false")
  /* default */ Instance<Boolean> authEnabled;

  @ConfigProperty(name = "initial.token.creation.enabled")
  /* default */ boolean initialTokenCreationEnabled;

  @ConfigProperty(name = "initial.token.user")
  /* default */ String initialTokenUser;

  @ConfigProperty(name = "initial.token.value")
  /* default */ String initialTokenValue;

  @ConfigProperty(name = "initial.token.secret")
  /* default */ String initialTokenSecret;

  @ConfigProperty(name = "initial.token.alias")
  /* default */ String initialTokenAlias;

  @Inject
  public UserApiServiceImpl(UserApiRepository repository) {
    this.repository = repository;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (demoMode.isEnabled()) {
      return;
    }
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantUserApi(
          this.initialTokenUser, this.initialTokenAlias, this.initialTokenValue, "testUrl", 0L);
      LOGGER.atDebug().log("Created default user API token.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantUserApi(
      final String uid,
      final String name,
      final String token,
      final String hostUrl,
      final Long expires) {
    // Avoid creating a duplicate on every restart
    if (this.tokenExists(uid, token)) {
      return;
    }
    final long createdAt = System.currentTimeMillis();

    final UserApi userApi = new UserApi(uid, name, token, hostUrl, createdAt, expires);
    try {
      this.repository.persist(userApi);
    } catch (final MongoWriteException e) {
      if (!DuplicateKeys.isDuplicateKey(e)) {
        throw e;
      }
      LOGGER.debug("Default user API token was created by another instance.");
    }
  }

  @Override
  public Collection<UserApi> getOwningTokens(final String uid) {
    return this.repository.findForUser(uid);
  }

  @Override
  public int deleteByValue(final String uid, final String token) {
    return this.repository.delete(DELETE_FLAG_QUERY, uid, token) > 0 ? 0 : -1;
  }

  @Override
  public boolean tokenExists(final String uid, final String token) {
    return this.repository.exists(uid, token, System.currentTimeMillis());
  }

  @Override
  public UserApi createNewUserApi(
      final String uid,
      final String name,
      final String token,
      final String hostUrl,
      final Long createdAt,
      final Long expires) {
    // An expired API token may be replaced even if it is not deleted yet
    this.repository.deleteExpired(uid, token, System.currentTimeMillis());
    final UserApi userApi = new UserApi(uid, name, token, hostUrl, createdAt, expires);
    this.repository.persist(userApi);

    return userApi;
  }
}
