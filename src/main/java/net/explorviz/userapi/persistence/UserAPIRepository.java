package net.explorviz.userapi.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import net.explorviz.userapi.model.UserAPI;
import java.util.Collection;

@ApplicationScoped
public class UserAPIRepository implements PanacheMongoRepositoryBase<UserAPI, String> {

    public Collection<UserAPI> findForUser(final String uId) {
      return this.list("uId", uId);
  }

    public Collection<UserAPI> findForUserAndToken(final String uId, final String token){
      return this.list("uid = ?1 and token = ?2", uId, token);
    }
}
