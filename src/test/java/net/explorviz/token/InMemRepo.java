package net.explorviz.token;

import io.quarkus.mongodb.panache.PanacheQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.explorviz.token.model.LandscapeToken;
import org.mockito.Mockito;

public class InMemRepo {

  private List<LandscapeToken> tokens = new ArrayList<>();

  public void addToken(LandscapeToken token) {
    this.tokens.add(token);
  }

  public Collection<LandscapeToken> findForUser(String uid) {
    return this.tokens.stream()
        .filter(t -> t.getOwnerId().equals(uid))
        .collect(Collectors.toList());
  }

  public PanacheQuery<LandscapeToken> findByValue(String value) {
    PanacheQuery mockPq = Mockito.mock(PanacheQuery.class);
    Mockito.when(mockPq.stream())
        .thenReturn(tokens.stream().filter(i -> i.getValue().equals(value))
        );
    return mockPq;
  }

  public long deleteByValue(String value) {
    boolean d = tokens.removeIf(t -> t.getValue().equals(value));
    return d ? 1L : 0L;
  }

  public int size() {
    return tokens.size();
  }


}
