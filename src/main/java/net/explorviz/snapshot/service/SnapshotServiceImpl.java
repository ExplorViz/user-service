package net.explorviz.snapshot.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

@ApplicationScoped
public class SnapshotServiceImpl implements SnapshotService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotService.class);

  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "owner = ?1 and createdAt = ?2 and isShared = ?3";
  private final SnapshotRepository repository;
  //TODO private final SnapshotEventService eventService;
  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  /* default */ Instance<Boolean> authEnabled; // NOCS
  @ConfigProperty(name = "initial.token.creation.enabled")
  /* default */ boolean initialTokenCreationEnabled; // NOCS
  @ConfigProperty(name = "initial.token.user")
  /* default */ String initialTokenUser; // NOCS
  @ConfigProperty(name = "initial.token.value")
  /* default */ String initialTokenValue; // NOCS
  @ConfigProperty(name = "initial.token.secret")
  /* default */ String initialTokenSecret; // NOCS
  @ConfigProperty(name = "initial.token.alias")
  /* default */ String initialTokenAlias; // NOCS

  @Inject
  public SnapshotServiceImpl(SnapshotRepository repository) {
    this.repository = repository;
  }

  void onStart(@Observes final StartupEvent ev) {
    if (this.initialTokenCreationEnabled) {
      this.createNewConstantSnapshot(this.initialTokenUser, this.initialTokenAlias);
      LOGGER.atDebug().log("Created default snapshot.");
    }
    LOGGER.atDebug().addArgument(authEnabled.get().equals("Quarkus OIDC is enabled: {}"));
  }

  private void createNewConstantSnapshot(final String owner, final String name) {
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document configuration = new Document();
    final Document camera = new Document();
    final Document annotations = new Document();
    final boolean isShared = false;
    final long deleteAt = 0L;
    final Document julius = new Document();

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, configuration, camera, annotations, isShared, deleteAt, julius);
    this.repository.persist(snapshot);
  }

  @Override
  public Collection<Snapshot> getOwningSnapshots(final String owner) {
    return this.repository.findForUser(owner);
  }

  @Override
  public int deleteByValue(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshotToDelete = this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    if (snapshotToDelete.size() != 1) {
      return -1;
    }

    this.repository.delete(DELETE_FLAG_QUERY, owner, createdAt, isShared);

    return 0;
  }

  @Override
  public boolean snapshotExists(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshots = this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    return !snapshots.isEmpty();
  }

  public Snapshot createNewSnapshot(final Snapshot snapshot){
    this.repository.persist(snapshot);
    return snapshot;
  }

  @Override
  public Snapshot getSnapshot(final String owner, final Long createdAt) {
    Collection<Snapshot> snapshot = this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, false);

    if (snapshot.size() != 1) {
      return null;
    }
    return snapshot.iterator().next();
  }
}
