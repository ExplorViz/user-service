package net.explorviz.userapi.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.UserAPIEvent;
import net.explorviz.token.service.TokenServiceImpl;
import net.explorviz.userapi.service.messaging.UserAPIEventService;
import net.explorviz.userapi.model.UserAPI;
import net.explorviz.userapi.persistence.UserAPIRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

@ApplicationScoped
public class UserAPIServiceImpl implements UserAPIService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);


  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "uId = ?1 AND token = ?2";
  private final UserAPIRepository repository;
  private final UserAPIEventService eventService;
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
  public UserAPIServiceImpl(UserAPIRepository repository, final UserAPIEventService eventService) {
    this.repository = repository;
    this.eventService = eventService;
  }

  /* default */ void onStart(@Observes final StartupEvent ev) {
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantUserAPI(this.initialTokenUser, this.initialTokenAlias,
          this.initialTokenValue, 0L);
      LOGGER.atDebug().log("Created default user API token.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get()).log("Quarkus OIDC is enabled: {}");
  }

  private void createNewConstantUserAPI(final String uId, final String name, final String token,
      final Long expires) {
    final long createdAt = System.currentTimeMillis();

    final UserAPI userAPI =
        new UserAPI(uId, name, token, createdAt, expires);
    this.repository.persist(userAPI);
    this.eventService.dispatch(new UserAPIEvent(EventType.CREATED, userAPI.toAvro()));
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

    UserAPI userAPI = userAPIToDelete.iterator().next();

    final long docsAffected = this.repository.delete(DELETE_FLAG_QUERY, uId, token);
    if (docsAffected == DELETE_FLAG) {
      this.eventService.dispatch(new UserAPIEvent(EventType.DELETED, userAPI.toAvro()));
    }

    return 0;
  }

  @Override
  public UserAPI createNewUserAPI(final String uId, final String name, final String token,
      final Long createdAt, final Long expires) {
    final UserAPI userAPI = new UserAPI(uId, name, token, createdAt, expires);
    this.repository.persist(userAPI);
    this.eventService.dispatch(new UserAPIEvent(EventType.CREATED, userAPI.toAvro()));
    return userAPI;
  }

}
