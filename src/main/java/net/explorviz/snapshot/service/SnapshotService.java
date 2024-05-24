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
   * Delete a snapshot.
   *
   * @param owner the given owner
   * @param createdAt creation date of the snapshot
   * @return number of deleted snapshots
   */
  int deleteByValue(String owner, Long createdAt);

  /**
   * Checks if a landscape with given owner and creation date exists.
   *
   * @param owner
   * @param createdAt
   * @return
   */
  boolean snapshotExists(String owner, Long createdAt);

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
   * @param owner
   * @param createdAt
   * @return
   */
  Snapshot getSnapshot(String owner, Long createdAt);
}
