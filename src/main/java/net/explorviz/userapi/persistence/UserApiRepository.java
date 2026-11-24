package net.explorviz.userapi.persistence;

import com.mongodb.client.model.CountOptions;
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

  /**
   * Checks whether a user has an API token that can still be used.
   *
   * @param uid the user id
   * @param token the API token
   * @param now the current time in epoch milliseconds
   * @return whether there is an API token that has not expired
   */
  public boolean exists(final String uid, final String token, final long now) {
    return this.mongoCollection()
            .countDocuments(
                Filters.and(
                    Filters.eq("uid", uid),
                    Filters.eq("token", token),
                    // Equivalent to UserApi.isExpired being false
                    Filters.or(
                        Filters.eq("expires", null),
                        Filters.eq("expires", 0L),
                        Filters.gte("expires", now))),
                new CountOptions().limit(1))
        > 0;
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
