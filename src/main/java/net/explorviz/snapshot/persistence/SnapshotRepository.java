package net.explorviz.snapshot.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import net.explorviz.snapshot.model.Snapshot;

@ApplicationScoped
public class SnapshotRepository implements PanacheMongoRepositoryBase<Snapshot, String> {

  public Collection<Snapshot> findForUser(final String owner) {
    return this.list("owner", owner);
  }

  public Collection<Snapshot> findForUserAndCreatedAtAndIsShared(final String owner,
      final Long createdAt, final boolean isShared) {
    return this.list("owner = ?1 and createdAt = ?2 and isShared = ?3",
        owner, createdAt, isShared);
  }

  public Collection<Snapshot> getAll() {
    return this.listAll();
  }
}
