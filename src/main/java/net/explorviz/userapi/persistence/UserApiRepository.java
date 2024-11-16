package net.explorviz.userapi.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import net.explorviz.userapi.model.UserApi;

@ApplicationScoped
public class UserApiRepository implements PanacheMongoRepositoryBase<UserApi, String> {

  public Collection<UserApi> findForUser(final String uid) {
    return this.list("uid", uid);
  }

  public Collection<UserApi> findForUserAndToken(final String uid, final String token) {
    return this.list("uid = ?1 and token = ?2", uid, token);
  }
}
