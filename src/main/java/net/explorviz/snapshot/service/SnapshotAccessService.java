package net.explorviz.snapshot.service;

import net.explorviz.snapshot.model.Snapshot;
import java.util.Arrays;

/**
 * Service for checking snapshot permissions.
 */
public interface SnapshotAccessService {

  SnapshotPermission[] getPermissions(Snapshot snapshot, String uId);

  /**
   * Checks whether a user can read a snapshot-
   *
   * @param snapshot the snapshot to read
   * @param uId the id of the user
   * @return {@code true} iff read access is granted to the user
   */
  default boolean canRead(final Snapshot snapshot, final String uId) {
    return Arrays.asList(this.getPermissions(snapshot, uId)).contains(SnapshotPermission.READ);
  }

  /**
   * Checks whether a user can delete a snapshot.
   *
   * @param snapshot the snapshot to delete
   * @param uId the id of the user
   * @return {@code true} iff the user is allowed to delete the snapshot
   */
  default boolean canDelete(final Snapshot snapshot, final String uId) {
    return Arrays.asList(this.getPermissions(snapshot, uId)).contains(SnapshotPermission.DELETE);
  }
}
