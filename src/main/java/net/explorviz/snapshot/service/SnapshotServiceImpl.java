package net.explorviz.snapshot.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;

@ApplicationScoped
public class SnapshotServiceImpl implements SnapshotService {

  private static final String DELETE_FLAG_QUERY = "owner = ?1 and createdAt = ?2 and isShared = ?3";
  private final SnapshotRepository repository;

  // TODO private final SnapshotEventService eventService;

  @Inject
  public SnapshotServiceImpl(SnapshotRepository repository) {
    this.repository = repository;
  }

  @Override
  public Collection<Snapshot> getOwningSnapshots(final String owner) {
    return this.repository.findForUser(owner);
  }

  @Override
  public Collection<Snapshot> getAllSnapshots() {
    return this.repository.getAll();
  }

  @Override
  public Collection<Snapshot> getVisibleSnapshots(final String user) {
    return this.repository.findVisibleTo(user);
  }

  @Override
  public int deleteByValue(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshotToDelete =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    // Delete all matches so that duplicates (e.g. from concurrent creation) can be removed
    if (snapshotToDelete.isEmpty()) {
      return -1;
    }

    this.repository.delete(DELETE_FLAG_QUERY, owner, createdAt, isShared);

    return 0;
  }

  @Override
  public boolean snapshotExists(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshots =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    return !snapshots.isEmpty();
  }

  @Override
  public Snapshot createNewSnapshot(final Snapshot snapshot) {
    this.repository.persist(snapshot);
    return snapshot;
  }

  @Override
  public Snapshot getSnapshot(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshot =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    // Tolerate duplicates (e.g. from concurrent creation) instead of hiding the snapshot
    return snapshot.isEmpty() ? null : snapshot.iterator().next();
  }

  @Override
  public void addNewSubscriber(final String owner, final Long createdAt, final String subscriber) {
    if (owner != null && !owner.equals(subscriber)) {
      this.repository.addSubscriber(owner, createdAt, subscriber);
    }
  }

  @Override
  public void removeSubscriber(final String owner, final Long createdAt, final String subscriber) {
    if (owner != null && !owner.equals(subscriber)) {
      this.repository.removeSubscriber(owner, createdAt, subscriber);
    }
  }

  @Override
  public int shareSnapshot(final String owner, final Long createdAt, final Long deleteAt) {
    final Collection<Snapshot> shared =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, true);

    if (!shared.isEmpty()) {
      final long now = System.currentTimeMillis();
      if (shared.stream().anyMatch(sn -> !Snapshot.isExpired(sn, now))) {
        return 1;
      }
      // The previous shared copy has expired, so it can be replaced
      this.repository.delete(DELETE_FLAG_QUERY, owner, createdAt, true);
    }

    final Collection<Snapshot> snapshot =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, false);

    if (snapshot.isEmpty()) {
      return -1;
    }

    Snapshot sn = snapshot.iterator().next();

    Snapshot sharedSnapshot =
        new Snapshot(
            sn.getOwner(),
            sn.getCreatedAt(),
            sn.getName(),
            sn.getLandscapeToken(),
            sn.getStructureData(),
            sn.getSerializedRoom(),
            sn.getTimestamps(),
            sn.getCamera(),
            true,
            sn.getSubscribedUsers(),
            deleteAt);

    this.repository.persist(sharedSnapshot);
    return 0;
  }
}
