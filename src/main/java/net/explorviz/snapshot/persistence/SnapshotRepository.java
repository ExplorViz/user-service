package net.explorviz.snapshot.persistence;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import net.explorviz.snapshot.model.Snapshot;
import java.util.Collection;

@ApplicationScoped
public class SnapshotRepository implements PanacheMongoRepositoryBase<Snapshot, String> {

  public Collection<Snapshot> findForUser(final String owner) {
    return this.list("owner", owner);
  }

  public Collection<Snapshot> findForUserAndCreatedAt(final String owner, final Long createdAt) {
    return this.list("owner = ?1 and createdAt = ?2", owner, createdAt);
  }
}
