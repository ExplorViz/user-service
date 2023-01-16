package net.explorviz.token;

import io.quarkus.mongodb.panache.PanacheQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.explorviz.token.model.LandscapeToken;
import org.mockito.Mockito;

public class InMemRepo {

  private final List<LandscapeToken> tokens = new ArrayList<>();

  public void addToken(final LandscapeToken token) {
    this.tokens.add(token);
  }

  public Collection<LandscapeToken> findForUser(final String uid) {
    return this.tokens.stream().filter(t -> t.getOwnerId().equals(uid))
        .collect(Collectors.toList());
  }

  public Collection<LandscapeToken> findSharedForUser(final String uid) {
    return this.tokens.stream().filter(t -> t.getSharedUsersIds().contains(uid))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public PanacheQuery<LandscapeToken> findByValue(final String value) {
    final PanacheQuery<LandscapeToken> mockPq = Mockito.mock(PanacheQuery.class);
    Mockito.when(mockPq.stream())
        .thenReturn(this.tokens.stream().filter(i -> i.getValue().equals(value)));
    return mockPq;
  }

  public long deleteByValue(final String value) {
    final boolean d = this.tokens.removeIf(t -> t.getValue().equals(value));
    return d ? 1L : 0L;
  }

  public int size() {
    return this.tokens.size();
  }


}
