package net.explorviz.snapshot.service;

import com.mongodb.MongoWriteException;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import net.explorviz.persistence.DuplicateKeys;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.model.SnapshotOverview;
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
  public SnapshotOverview getVisibleSnapshots(final String user, final Page page) {
    final long now = System.currentTimeMillis();
    return new SnapshotOverview(
        this.repository.findPersonal(user, now, page),
        this.repository.findShared(user, now, page),
        this.repository.findSubscribed(user, now, page));
  }

  @Override
  public int deleteByValue(final String owner, final Long createdAt, final boolean isShared) {
    return this.repository.delete(DELETE_FLAG_QUERY, owner, createdAt, isShared) > 0 ? 0 : -1;
  }

  @Override
  public boolean snapshotExists(final String owner, final Long createdAt, final boolean isShared) {
    return this.repository.exists(owner, createdAt, isShared);
  }

  @Override
  public Snapshot createNewSnapshot(final Snapshot snapshot) {
    this.repository.persist(snapshot);
    return snapshot;
  }

  @Override
  public Snapshot getSnapshot(final String owner, final Long createdAt, final boolean isShared) {
    return this.repository.findSnapshot(owner, createdAt, isShared).orElse(null);
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
    final long now = System.currentTimeMillis();
    if (this.repository.isShared(owner, createdAt, now)) {
      return 1;
    }

    final Optional<Snapshot> personal = this.repository.findSnapshot(owner, createdAt, false);
    if (personal.isEmpty()) {
      return -1;
    }
    final Snapshot sn = personal.get();

    // A previous shared copy that has expired can be replaced
    this.repository.deleteExpiredShared(owner, createdAt, now);

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

    try {
      this.repository.persist(sharedSnapshot);
    } catch (final MongoWriteException e) {
      if (DuplicateKeys.isDuplicateKey(e)) {
        // Shared by a concurrent request
        return 1;
      }
      throw e;
    }
    return 0;
  }
}
