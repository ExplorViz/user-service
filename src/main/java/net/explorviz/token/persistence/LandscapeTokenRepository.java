package net.explorviz.token.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import net.explorviz.token.model.LandscapeToken;

/**
 * MongoRepository for {@link LandscapeToken}s.
 */
@ApplicationScoped
public class LandscapeTokenRepository
    implements PanacheMongoRepositoryBase<LandscapeToken, String> {

  public Collection<LandscapeToken> findForUser(final String userId) {
    return this.list("owner", userId);
  }

  public Collection<LandscapeToken> findSharedForUser(final String userId) {
    return this.list("sharedUsers in ?1", userId);
  }
}
