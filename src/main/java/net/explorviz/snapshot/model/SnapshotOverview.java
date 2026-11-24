package net.explorviz.snapshot.model;

import java.util.List;

/**
 * The snapshots a user can see, newest first.
 *
 * @param personalSnapshots snapshots of the user that are not shared
 * @param sharedSnapshots snapshots shared by the user
 * @param subscribedSnapshots snapshots of others that the user has subscribed to
 */
public record SnapshotOverview(
    List<SnapshotSummary> personalSnapshots,
    List<SnapshotSummary> sharedSnapshots,
    List<SnapshotSummary> subscribedSnapshots) {}
