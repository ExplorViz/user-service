package net.explorviz.snapshot.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);
    this.repository.persist(snapshot);
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
  public int deleteByValue(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshotToDelete =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    if (snapshotToDelete.size() != 1) {
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

  public Snapshot createNewSnapshot(final Snapshot snapshot) {
    this.repository.persist(snapshot);
    return snapshot;
  }

  @Override
  public Snapshot getSnapshot(final String owner, final Long createdAt, final boolean isShared) {
    Collection<Snapshot> snapshot =
        this.repository.findForUserAndCreatedAtAndIsShared(owner, createdAt, isShared);

    if (snapshot.size() != 1) {
      return null;
    }
    return snapshot.iterator().next();
  }

  @Override
  public void addNewSubscriber(final String owner, final Long createdAt, final String subscriber) {
    if (!owner.equals(subscriber)) {
      Collection<Snapshot> sn = this.repository.findForUserAndCreatedAtAndIsShared(owner,
          createdAt, true);

      if (sn.size() != 1) {
        return;
      }

      Snapshot snapshot = sn.iterator().next();

      if (snapshot != null) {

        Document subscribedUsers = snapshot.getSubscribedUsers();

        ArrayList<String> subscriberList;
        try {
          subscriberList = (ArrayList<String>) subscribedUsers.get("subscriberList");
        } catch (Exception e) {
          System.out.println("Something went wrong with the subscriberList");
          return;
        }

        if (!subscriberList.contains(subscriber)) {
          subscriberList.add(subscriber);

          Document newSubs = new Document();
          newSubs.append("subscriberList", subscriberList);

          Snapshot newSnapshot =
              new Snapshot(snapshot.getOwner(), snapshot.getCreatedAt(), snapshot.getName(),
                  snapshot.getLandscapeToken(), snapshot.getStructureData(),
                  snapshot.getSerializedRoom(), snapshot.getTimestamps(), snapshot.getCamera(),
                  snapshot.getIsShared(), newSubs, snapshot.getDeleteAt());

          // - AutomaticPojoCodec has problems with reading Arrays from MongoDB
          // - MongoDB has problems with updating lists -> lists will become Strings
          // - MongoDB has problems with updating Documents -> Finds '}' while expecting ':' (?)
          // => Solution: Deletion of old snapshot and creation of new with updated subscriber list
          //    (not very beautiful because of more heavy database operation, but for now the only
          //     working solution)
          this.repository.delete(DELETE_FLAG_QUERY, snapshot.getOwner(), snapshot.getCreatedAt(),
              snapshot.getIsShared());
          this.repository.persist(newSnapshot);
        }
      }
    }
  }

  @Override
  public void removeSubscriber(final String owner, final Long createdAt, final String subscriber) {
    if (!owner.equals(subscriber)) {
      Collection<Snapshot> sn = this.repository.findForUserAndCreatedAtAndIsShared(owner,
          createdAt, true);

      if (sn.size() != 1) {
        return;
      }

      Snapshot snapshot = sn.iterator().next();

      if (snapshot != null) {

        Document subscribedUsers = snapshot.getSubscribedUsers();

        ArrayList<String> subscriberList;
        try {
          subscriberList = (ArrayList<String>) subscribedUsers.get("subscriberList");
        } catch (Exception e) {
          System.out.println("Something went wrong with the subscriberList");
          return;
        }

        if (subscriberList.contains(subscriber)) {
          subscriberList.remove(subscriber);

          Document newSubs = new Document();
          newSubs.append("subscriberList", subscriberList);

          Snapshot newSnapshot =
              new Snapshot(snapshot.getOwner(), snapshot.getCreatedAt(), snapshot.getName(),
                  snapshot.getLandscapeToken(), snapshot.getStructureData(),
                  snapshot.getSerializedRoom(), snapshot.getTimestamps(), snapshot.getCamera(),
                  snapshot.getIsShared(), newSubs, snapshot.getDeleteAt());

          // - AutomaticPojoCodec has problems with reading Arrays from MongoDB
          // - MongoDB has problems with updating lists -> lists will become Strings
          // - MongoDB has problems with updating Documents -> Finds '}' while expecting ':' (?)
          // => Solution: Deletion of old snapshot and creation of new with updated subscriber list
          //    (not very beautiful because of more heavy database operation, but for now the only
          //     working solution)
          this.repository.delete(DELETE_FLAG_QUERY, snapshot.getOwner(), snapshot.getCreatedAt(),
              snapshot.getIsShared());
          this.repository.persist(newSnapshot);
        }
      }
    }
  }

  @Override
  public int shareSnapshot(final String owner, final Long createdAt, final Long deleteAt) {
    Collection<Snapshot> snapshot = this.repository.findForUserAndCreatedAtAndIsShared(owner,
        createdAt, true);

    if (snapshot.size() == 1) {
      return 1;
    }

    snapshot = this.repository.findForUserAndCreatedAtAndIsShared(owner,
        createdAt, false);

    if (snapshot.size() != 1) {
      return -1;
    }

    Snapshot sn = snapshot.iterator().next();

    Snapshot sharedSnapshot = new Snapshot(sn.getOwner(), sn.getCreatedAt(),
        sn.getName(), sn.getLandscapeToken(), sn.getStructureData(),
        sn.getSerializedRoom(), sn.getTimestamps(), sn.getCamera(),
        true, sn.getSubscribedUsers(), deleteAt);

    this.repository.persist(sharedSnapshot);
    return 0;
  }
}
