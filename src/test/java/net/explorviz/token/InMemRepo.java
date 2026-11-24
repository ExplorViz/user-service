package net.explorviz.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.explorviz.token.model.LandscapeToken;

public class InMemRepo {

  private final List<LandscapeToken> tokens = new ArrayList<>();

  public void addToken(final LandscapeToken token) {
    this.tokens.add(token);
  }

  public Collection<LandscapeToken> findForUser(final String uid) {
    return this.tokens.stream()
        .filter(t -> t.getOwnerId().equals(uid))
        .collect(Collectors.toList());
  }

  public Collection<LandscapeToken> findSharedForUser(final String uid) {
    return this.tokens.stream()
        .filter(t -> t.getSharedUsersIds().contains(uid))
        .collect(Collectors.toList());
  }

  public Optional<LandscapeToken> findByValue(final String value) {
    return this.tokens.stream().filter(t -> t.getValue().equals(value)).findFirst();
  }

  public boolean deleteByValue(final String value) {
    return this.tokens.removeIf(t -> t.getValue().equals(value));
  }

  public int size() {
    return this.tokens.size();
  }
}
