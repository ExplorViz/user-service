package net.explorviz.token.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
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

}
