package net.explorviz.userapi.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import net.explorviz.userapi.model.UserAPI;
import net.explorviz.userapi.persistence.UserAPIRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

@ApplicationScoped
public class UserAPIServiceImpl implements UserAPIService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserAPIService.class);


  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "uid = ?1 and token = ?2";
  private final UserAPIRepository repository;
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
  public UserAPIServiceImpl(UserAPIRepository repository) {
    this.repository = repository;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantUserAPI(this.initialTokenUser, this.initialTokenAlias,
          this.initialTokenValue, "testUrl", 0L);
      LOGGER.atDebug().log("Created default user API token.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantUserAPI(final String uId, final String name, final String token,
      final String hostUrl, final Long expires) {
    final long createdAt = System.currentTimeMillis();

    final UserAPI userAPI =
        new UserAPI(uId, name, token, hostUrl, createdAt, expires);
    this.repository.persist(userAPI);
  }

  @Override
  public Collection<UserAPI> getOwningTokens(final String uId) {
    return this.repository.findForUser(uId);
  }

  @Override
  public int deleteByValue(final String uId, final String token) {
    Collection<UserAPI> userAPIToDelete = this.repository.findForUserAndToken(uId, token);

    if (userAPIToDelete.size() != 1){
      return -1;
    }

    this.repository.delete(DELETE_FLAG_QUERY, uId, token);

    return 0;
  }

  @Override
  public boolean tokenExists(final String uId, final String token) {
    Collection<UserAPI> tokenUId = this.repository.findForUserAndToken(uId, token);

    return !tokenUId.isEmpty();
  }

  @Override
  public UserAPI createNewUserAPI(final String uId, final String name, final String token,
     final String hostUrl, final Long createdAt, final Long expires) {
    final UserAPI userAPI = new UserAPI(uId, name, token, hostUrl, createdAt, expires);
    this.repository.persist(userAPI);

    return userAPI;
  }

}
