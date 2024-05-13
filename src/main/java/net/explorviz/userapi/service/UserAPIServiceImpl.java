package net.explorviz.userapi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.UserAPIEvent;
import net.explorviz.userapi.service.messaging.EventService;
import net.explorviz.userapi.model.UserAPI;
import net.explorviz.userapi.persistence.UserAPIRepository;
import java.util.Collection;

@ApplicationScoped
public class UserAPIServiceImpl implements UserAPIService {

  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "uId = ?1 AND token = ?2";
  private final UserAPIRepository repository;
  private final EventService eventService;

  @Inject
  public UserAPIServiceImpl(UserAPIRepository repository, final EventService eventService) {
    this.repository = repository;
    this.eventService = eventService;
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
