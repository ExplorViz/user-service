package net.explorviz.token;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;

public class TestUtils {

  /**
   * In-Mem mock implementation for {@link LandscapeTokenRepository}
   */
  public static class MockRepo extends LandscapeTokenRepository {

    private Collection<LandscapeToken> tokens;


    public MockRepo() {
      this.tokens = new HashSet<>();
    }

    public Collection<LandscapeToken> getTokens() {
      return tokens;
    }

    @Override
    public Collection<LandscapeToken> findForUser(final String userId) {
      return tokens.stream().filter(t -> t.getOwnerId().equals(userId))
          .collect(Collectors.toList());
    }

    @Override
    public void persist(final LandscapeToken token) {
      this.tokens.add(token);
    }
  }

}
