package net.explorviz.token.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;

@ApplicationScoped
public class LandscapeTokenRepository
    implements PanacheMongoRepositoryBase<LandscapeToken, String> {

  public Collection<LandscapeToken> findForUser(final String userId) {
    return list("owner", userId);
  }

}
