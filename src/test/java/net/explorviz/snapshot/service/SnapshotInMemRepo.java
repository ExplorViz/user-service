package net.explorviz.snapshot.service;

import io.quarkus.panache.common.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.model.SnapshotSummary;

public class SnapshotInMemRepo {

  private final List<Snapshot> snapshots = new ArrayList<>();

  public void addSnapshot(final Snapshot snapshot) {
    this.snapshots.add(snapshot);
  }

  public Collection<Snapshot> findForUser(final String owner) {
    return this.snapshots.stream()
        .filter(s -> s.getOwner().equals(owner))
        .collect(Collectors.toList());
  }

  public Optional<Snapshot> findSnapshot(
      final String owner, final long createdAt, final boolean isShared) {
    return this.snapshots.stream()
        .filter(s -> owner.equals(s.getOwner()))
        .filter(s -> s.getCreatedAt() == createdAt && s.getIsShared() == isShared)
        .findFirst();
  }

  public boolean isShared(final String owner, final long createdAt, final long now) {
    return this.findSnapshot(owner, createdAt, true)
        .filter(s -> !Snapshot.isExpired(s, now))
        .isPresent();
  }

  public List<SnapshotSummary> findOwned(
      final String owner, final boolean isShared, final long now, final Page page) {
    return summaries(s -> owner.equals(s.getOwner()) && s.getIsShared() == isShared, now, page);
  }

  public List<SnapshotSummary> findSubscribed(final String user, final long now, final Page page) {
    return summaries(
        s -> {
          final List<String> subs =
              s.getSubscribedUsers() == null
                  ? null
                  : s.getSubscribedUsers().getList("subscriberList", String.class);
          return subs != null && subs.contains(user) && !user.equals(s.getOwner());
        },
        now,
        page);
  }

  private List<SnapshotSummary> summaries(
      final Predicate<Snapshot> filter, final long now, final Page page) {
    return this.snapshots.stream()
        .filter(filter)
        .filter(s -> !Snapshot.isExpired(s, now))
        .sorted(Comparator.comparing(Snapshot::getCreatedAt).reversed())
        .skip((long) page.index * page.size)
        .limit(page.size)
        .map(
            s ->
                new SnapshotSummary(
                    s.getOwner(), s.getCreatedAt(), s.getName(), s.getLandscapeToken()))
        .toList();
  }

  public Collection<Snapshot> getAll() {
    return this.snapshots;
  }

  public long deleteByValue(final String owner, final long createdAt, final boolean isShared) {
    final boolean d =
        this.snapshots.removeIf(
            s ->
                s.getOwner().equals(owner)
                    && s.getCreatedAt() == createdAt
                    && s.getIsShared() == isShared);
    return d ? 1L : 0L;
  }

  public int size() {
    return this.snapshots.size();
  }
}
