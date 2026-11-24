package net.explorviz.snapshot.persistence;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collection;
import net.explorviz.snapshot.model.Snapshot;
import org.bson.Document;
import org.bson.conversions.Bson;

@ApplicationScoped
public class SnapshotRepository implements PanacheMongoRepositoryBase<Snapshot, String> {

  private static final String SUBSCRIBERS = "subscribedUsers";
  private static final String SUBSCRIBER_LIST = SUBSCRIBERS + ".subscriberList";

  public Collection<Snapshot> findForUser(final String owner) {
    return this.list("owner", owner);
  }

  public Collection<Snapshot> findForUserAndCreatedAtAndIsShared(
      final String owner, final Long createdAt, final boolean isShared) {
    return this.list("owner = ?1 and createdAt = ?2 and isShared = ?3", owner, createdAt, isShared);
  }

  /**
   * Retrieve all snapshots a user owns or has subscribed to.
   *
   * @param user the user id
   * @return snapshots owned by or subscribed to by the user
   */
  public Collection<Snapshot> findVisibleTo(final String user) {
    return this.list("{ $or: [ { owner: ?1 }, { '" + SUBSCRIBER_LIST + "': ?1 } ] }", user);
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
            Filters.and(Filters.eq("owner", owner), Filters.eq("landscapeToken.value", tokenValue)))
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
        .deleteMany(Filters.and(Filters.gt("deleteAt", 0L), Filters.lt("deleteAt", now)))
        .getDeletedCount();
  }

  private static Bson sharedSnapshot(final String owner, final Long createdAt) {
    return Filters.and(
        Filters.eq("owner", owner),
        Filters.eq("createdAt", createdAt),
        Filters.eq("isShared", true));
  }
}
