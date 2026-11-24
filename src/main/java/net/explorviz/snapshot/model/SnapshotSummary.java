package net.explorviz.snapshot.model;

import org.bson.Document;

/**
 * The part of a {@link Snapshot} that is shown in lists.
 *
 * @param owner the creator of the snapshot
 * @param createdAt the time the snapshot was created
 * @param name the name of the snapshot
 * @param landscapeToken the landscape token of the corresponding landscape
 */
public record SnapshotSummary(String owner, Long createdAt, String name, Document landscapeToken) {}
