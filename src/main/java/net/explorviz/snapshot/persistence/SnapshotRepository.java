package net.explorviz.snapshot.persistence;

import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.model.SnapshotSummary;
import org.bson.Document;
import org.bson.conversions.Bson;

@ApplicationScoped
public class SnapshotRepository implements PanacheMongoRepositoryBase<Snapshot, String> {

  private static final String OWNER = "owner";
  private static final String CREATED_AT = "createdAt";
  private static final String NAME = "name";
  private static final String LANDSCAPE_TOKEN = "landscapeToken";
  private static final String IS_SHARED = "isShared";
  private static final String DELETE_AT = "deleteAt";
  private static final String SUBSCRIBERS = "subscribedUsers";
  private static final String SUBSCRIBER_LIST = SUBSCRIBERS + ".subscriberList";

  public Collection<Snapshot> findForUser(final String owner) {
    return this.list("owner", owner);
  }

  public Optional<Snapshot> findSnapshot(
      final String owner, final Long createdAt, final boolean isShared) {
    return this.find("owner = ?1 and createdAt = ?2 and isShared = ?3", owner, createdAt, isShared)
        .firstResultOptional();
  }

  public boolean exists(final String owner, final Long createdAt, final boolean isShared) {
    return this.count("owner = ?1 and createdAt = ?2 and isShared = ?3", owner, createdAt, isShared)
        > 0;
  }

  /**
   * Checks whether there is a shared snapshot that can still be opened.
   *
   * @param owner owner of the snapshot
   * @param createdAt creation date of the snapshot
   * @param now the current time in epoch milliseconds
   * @return whether there is a shared snapshot that has not expired
   */
  public boolean isShared(final String owner, final Long createdAt, final long now) {
    return this.mongoCollection()
            .countDocuments(
                Filters.and(sharedSnapshot(owner, createdAt), notExpired(now)),
                new CountOptions().limit(1))
        > 0;
  }

  /**
   * Retrieves the snapshots of a user that are not shared, newest first.
   *
   * @param owner the user id
   * @param now the current time in epoch milliseconds
   * @param page the requested part of the list
   * @return snapshots that have not expired
   */
  public List<SnapshotSummary> findPersonal(final String owner, final long now, final Page page) {
    return this.summaries(
        Filters.and(Filters.eq(OWNER, owner), Filters.ne(IS_SHARED, true), notExpired(now)),
        Sorts.descending(CREATED_AT),
        page);
  }

  /**
   * Retrieves the snapshots a user has shared, newest first.
   *
   * @param owner the user id
   * @param now the current time in epoch milliseconds
   * @param page the requested part of the list
   * @return snapshots that have not expired
   */
  public List<SnapshotSummary> findShared(final String owner, final long now, final Page page) {
    return this.summaries(
        Filters.and(Filters.eq(OWNER, owner), Filters.eq(IS_SHARED, true), notExpired(now)),
        Sorts.descending(CREATED_AT),
        page);
  }

  /**
   * Retrieves the snapshots of others that a user has subscribed to, newest first.
   *
   * @param user the user id
   * @param now the current time in epoch milliseconds
   * @param page the requested part of the list
   * @return snapshots that have not expired
   */
  public List<SnapshotSummary> findSubscribed(final String user, final long now, final Page page) {
    return this.summaries(
        Filters.and(Filters.eq(SUBSCRIBER_LIST, user), Filters.ne(OWNER, user), notExpired(now)),
        // Snapshots of different owners may have the same date, but pages need a definite order
        Sorts.descending(CREATED_AT, OWNER),
        page);
  }

  private List<SnapshotSummary> summaries(final Bson filter, final Bson sort, final Page page) {
    return this.mongoCollection()
        .withDocumentClass(Document.class)
        .find(filter)
        .projection(Projections.include(OWNER, CREATED_AT, NAME, LANDSCAPE_TOKEN))
        .sort(sort)
        .skip(page.index * page.size)
        .limit(page.size)
        .map(SnapshotRepository::toSummary)
        .into(new ArrayList<>());
  }

  private static SnapshotSummary toSummary(final Document document) {
    final Number createdAt = document.get(CREATED_AT, Number.class);
    return new SnapshotSummary(
        document.getString(OWNER),
        createdAt == null ? null : createdAt.longValue(),
        document.getString(NAME),
        document.get(LANDSCAPE_TOKEN, Document.class));
  }

  public Collection<Snapshot> getAll() {
    return this.listAll();
  }

  /**
   * Atomically adds a subscriber to a shared snapshot.
   *
   * @param owner owner of the snapshot
   * @param createdAt creation date of the snapshot
   * @param subscriber the user to add
   */
  public void addSubscriber(final String owner, final Long createdAt, final String subscriber) {
    final Bson snapshot = sharedSnapshot(owner, createdAt);
    // $addToSet cannot create a field inside a null value, so initialize it first
    this.mongoCollection()
        .withDocumentClass(Document.class)
        .updateMany(
            Filters.and(snapshot, Filters.eq(SUBSCRIBERS, null)),
            Updates.set(SUBSCRIBERS, new Document("subscriberList", new ArrayList<String>())));
    this.mongoCollection()
        .withDocumentClass(Document.class)
        .updateMany(snapshot, Updates.addToSet(SUBSCRIBER_LIST, subscriber));
  }

  /**
   * Atomically removes a subscriber from a shared snapshot.
   *
   * @param owner owner of the snapshot
   * @param createdAt creation date of the snapshot
   * @param subscriber the user to remove
   */
  public void removeSubscriber(final String owner, final Long createdAt, final String subscriber) {
    this.mongoCollection()
        .withDocumentClass(Document.class)
        .updateMany(
            Filters.and(sharedSnapshot(owner, createdAt), Filters.type(SUBSCRIBER_LIST, "array")),
            Updates.pull(SUBSCRIBER_LIST, subscriber));
  }

  /**
   * Deletes the snapshots that the owner of a landscape token created of the landscape. Snapshots
   * of other users are kept: they belong to these users and can be viewed without the landscape.
   *
   * @param owner owner of the landscape token
   * @param tokenValue value of the landscape token
   * @return number of deleted snapshots
   */
  public long deleteForToken(final String owner, final String tokenValue) {
    return this.mongoCollection()
        .deleteMany(
            Filters.and(
                Filters.eq(OWNER, owner), Filters.eq(LANDSCAPE_TOKEN + ".value", tokenValue)))
        .getDeletedCount();
  }

  /**
   * Deletes all snapshots whose expiration date has passed.
   *
   * @param now the current time in epoch milliseconds
   * @return number of deleted snapshots
   */
  public long deleteExpired(final long now) {
    return this.mongoCollection()
        .deleteMany(Filters.and(Filters.gt(DELETE_AT, 0L), Filters.lt(DELETE_AT, now)))
        .getDeletedCount();
  }

  /**
   * Deletes a shared snapshot if its expiration date has passed.
   *
   * @param owner owner of the snapshot
   * @param createdAt creation date of the snapshot
   * @param now the current time in epoch milliseconds
   */
  public void deleteExpiredShared(final String owner, final Long createdAt, final long now) {
    this.mongoCollection()
        .deleteMany(
            Filters.and(
                sharedSnapshot(owner, createdAt),
                Filters.ne(DELETE_AT, 0L),
                Filters.lt(DELETE_AT, now)));
  }

  /** Equivalent to {@link Snapshot#isExpired(Snapshot, long)} being false. */
  private static Bson notExpired(final long now) {
    return Filters.or(
        Filters.eq(DELETE_AT, null), Filters.eq(DELETE_AT, 0L), Filters.gte(DELETE_AT, now));
  }

  private static Bson sharedSnapshot(final String owner, final Long createdAt) {
    return Filters.and(
        Filters.eq(OWNER, owner), Filters.eq(CREATED_AT, createdAt), Filters.eq(IS_SHARED, true));
  }
}
