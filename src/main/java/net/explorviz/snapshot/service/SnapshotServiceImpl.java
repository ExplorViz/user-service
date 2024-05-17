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
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;

@ApplicationScoped
public class SnapshotServiceImpl implements SnapshotService {

//  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotService.class);

  private static final int DELETE_FLAG = 1;
  private static final String DELETE_FLAG_QUERY = "owner = ?1 and createdAt = ?2";
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

  /**
   * TODO: Add EventService if needed
   *
   */
  @Inject
  public SnapshotServiceImpl(SnapshotRepository repository) {
    this.repository = repository;
  }

//  void onStart(@Observes final StartupEvent ev) {
//    if (this.initialTokenCreationEnabled) {
//      this.createNewConstantSnapshot(this.initialTokenUser, this.initialTokenAlias);
//      LOGGER.atDebug().log("Created default snapshot.");
//    }
//    LOGGER.atDebug().addArgument(authEnabled.get().equals("Quarkus OIDC is enabled: {}"));
//  }

//  private void createNewConstantSnapshot(final String owner, final String name) {
//    final long createdAt = System.currentTimeMillis();
//    final Document landscapeToken = new Document();
//    final Document structureData = new Document();
//    final Document configuration = new Document();
//    final Document camera = new Document();
//    final Document[] annotations = new Document[0];
//    final boolean isShared = false;
//    final long deleteAt = 0L;
//    final Document julius = new Document();
//
//    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
//        structureData, configuration, camera, annotations, isShared, deleteAt, julius);
//    this.repository.persist(snapshot);
//    // this.eventService.dispatch(new SnapshotEvent(EvenType.CREATED, snapshot.toAvro()));
//  }

  @Override
  public Collection<Snapshot> getOwningSnapshots(final String owner) {
    return this.repository.findForUser(owner);
  }

  @Override
  public int deleteByValue(final String owner, final Long createdAt) {
    Collection<Snapshot> snapshotToDelete = this.repository.findForUserAndCreatedAt(owner, createdAt);

    if (snapshotToDelete.size() != 1) {
      return -1;
    }

    Snapshot snapshot = snapshotToDelete.iterator().next();

    final long docsAffected = this.repository.delete(DELETE_FLAG_QUERY, owner, createdAt);
    // TODO: If required
    //if (docsAffected == DELETE_FLAG) {
    //  this.eventService.dispatch(new SnapshotEvent(EventType.DELETED, snapshot.toAvro()));
    //}

    return 0;
  }

  @Override
  public boolean snapshotExists(final String owner, final Long createdAt) {
    Collection<Snapshot> snapshots = this.repository.findForUserAndCreatedAt(owner, createdAt);

    return !snapshots.isEmpty();
  }

  @Override
  public Snapshot createNewSnapshot(final String owner, final Long createdAt, final String name,
      final Document landscapeToken, final Document structureData, final Document configuration,
      final Document camera, final Document annotations, final boolean isShared,
      final Long deleteAt, final Document julius) {
    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken, structureData,
        configuration, camera, annotations, isShared, deleteAt, julius);
    this.repository.persist(snapshot);
    // this.eventService.dispatch(new SnapshotEvent(EventType.CREATED, snapshot.toAvro()));
    return snapshot;
  }

  public Snapshot test(final Snapshot snapshot){
    this.repository.persist(snapshot);
    return snapshot;
  }
}
