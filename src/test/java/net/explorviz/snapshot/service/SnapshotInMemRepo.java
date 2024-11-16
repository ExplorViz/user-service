package net.explorviz.snapshot.service;

import net.explorviz.snapshot.model.Snapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SnapshotInMemRepo {

  private final List<Snapshot> snapshots = new ArrayList<>();

  public void addSnapshot(final Snapshot snapshot) {
    this.snapshots.add(snapshot);
  }

  public Collection<Snapshot> findForUser(final String owner) {
    return this.snapshots.stream().filter(s -> s.getOwner().equals(owner))
        .collect(Collectors.toList());
  }

  public Collection<Snapshot> findForUserAndCreatedAtAndIsShared(
      final String owner,
      final long createdAt,
      final boolean isShared
  ) {
    return this.snapshots.stream().filter(s ->
        s.getOwner().equals(owner) && s.getCreatedAt() == createdAt && s.getIsShared() == isShared)
        .collect(Collectors.toList());
  }

  public Collection<Snapshot> getAll() {
    return this.snapshots;
  }

  public long deleteByValue(final String owner, final long createdAt, final boolean isShared) {
    final boolean d =
        this.snapshots.removeIf(s -> s.getOwner().equals(owner) && s.getCreatedAt() == createdAt
            && s.getIsShared() == isShared);
    return d ? 1L : 0L;
  }

  public int size() {
    return this.snapshots.size();
  }
}
