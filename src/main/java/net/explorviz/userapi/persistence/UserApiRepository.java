package net.explorviz.userapi.persistence;

import com.mongodb.client.model.Filters;
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

  /**
   * Deletes all API tokens whose expiration date has passed.
   *
   * @param now the current time in epoch milliseconds
   * @return number of deleted API tokens
   */
  public long deleteExpired(final long now) {
    return this.mongoCollection()
        .deleteMany(Filters.and(Filters.gt("expires", 0L), Filters.lt("expires", now)))
        .getDeletedCount();
  }

  /**
   * Deletes an API token of a user if its expiration date has passed.
   *
   * @param uid the user id
   * @param token the API token
   * @param now the current time in epoch milliseconds
   */
  public void deleteExpired(final String uid, final String token, final long now) {
    this.mongoCollection()
        .deleteMany(
            Filters.and(
                Filters.eq("uid", uid),
                Filters.eq("token", token),
                Filters.gt("expires", 0L),
                Filters.lt("expires", now)));
  }
}
