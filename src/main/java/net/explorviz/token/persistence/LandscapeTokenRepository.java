package net.explorviz.token.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;

@ApplicationScoped
public class LandscapeTokenRepository implements PanacheMongoRepository<LandscapeToken> {

  public Collection<LandscapeToken> findForUser(final String userId) {
    return list("owner", userId);
  }

}
