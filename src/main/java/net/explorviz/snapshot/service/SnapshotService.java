package net.explorviz.snapshot.service;

import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.resources.SnapshotResource;
import org.bson.Document;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import java.util.Collection;

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
   * @return
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
   * @return
   */
  boolean snapshotExists(String owner, Long createdAt, boolean isShared);

  /**
   * Creates a new snapshots.
   *
   * @param snapshot
   * @return the created snapshot object
   */
  Snapshot createNewSnapshot(Snapshot snapshot);

  /**
   * Retrieve snapshot of an owner.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @param isShared whether snapshot is shared or not
   * @return
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
   * @return
   */
  int shareSnapshot(String owner, Long createdAt, Long deleteAt);
}
