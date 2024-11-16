package net.explorviz.snapshot.service;

import java.util.Collection;
import net.explorviz.snapshot.model.Snapshot;

/**
 * Interface to manage {@link Snapshot}s.
 */
public interface SnapshotService {

  /**
   * Retrieve all snapshots owned by given user.
   *
   * @param owner the given owner
   * @return collection of all snapshots owned by given user
   */
  Collection<Snapshot> getOwningSnapshots(String owner);

  /**
   * Retrieve all snapshots of the database.
   *
   * @return a collection of all snapshots in the database
   */
  Collection<Snapshot> getAllSnapshots();

  /**
   * Delete a snapshot.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param isShared whether snapshot is shared or not
   * @return number of deleted snapshots
   */
  int deleteByValue(String owner, Long createdAt, boolean isShared);

  /**
   * Checks if a landscape with given owner and creation date exists.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param isShared whether snapshot is shared or not
   * @return whether a snapshot exists or not
   */
  boolean snapshotExists(String owner, Long createdAt, boolean isShared);

  /**
   * Creates a new snapshots.
   *
   * @param snapshot The given snapshot
   * @return the created snapshot object
   */
  Snapshot createNewSnapshot(Snapshot snapshot);

  /**
   * Retrieve snapshot of an owner.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param isShared whether snapshot is shared or not
   * @return the snapshot corresponding to the given data
   */
  Snapshot getSnapshot(String owner, Long createdAt, boolean isShared);

  /**
   * Adds a subscriber to the subscriber list of a snapshot.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param subscriber the given subscriber
   */
  void addNewSubscriber(String owner, Long createdAt, String subscriber);

  /**
   * Removes a subscriber of the subscriber list of a snapshot.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param subscriber the given subscriber
   */
  void removeSubscriber(String owner, Long createdAt, String subscriber);

  /**
   * Creates a copy of the snapshot with given owner and createdAt with isShared is true.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @return returns 0 if shared, -1 if something was wrong with given data and 1 if already shared
   */
  int shareSnapshot(String owner, Long createdAt, Long deleteAt);
}
