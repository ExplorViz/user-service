package net.explorviz.token.persistence;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.explorviz.proto.EventType;
import net.explorviz.token.model.LandscapeToken;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * MongoRepository for {@link LandscapeToken}s.
 *
 * <p>Changes of a token and the resulting events are written atomically, since events are stored in
 * the document of the token until they are published. Hence, documents of deleted tokens remain
 * until their deletion event is published.
 */
@ApplicationScoped
public class LandscapeTokenRepository
    implements PanacheMongoRepositoryBase<LandscapeToken, String> {

  /** Until this time (epoch milliseconds), pending events are published by someone else. */
  public static final String LEASE_UNTIL = "outbox.leaseUntil";

  private static final String ID = "_id";
  private static final String VALUE = "value";
  private static final String ALIAS = "alias";
  private static final String SHARED_USERS = "sharedUsers";
  private static final String DELETED = "deleted";
  private static final String OUTBOX = "outbox";
  private static final String EVENTS = "outbox.events";
  private static final String NOT_DELETED = DELETED + ": { $ne: true }";

  public Optional<LandscapeToken> findByValue(final String value) {
    return this.find("{ value: ?1, " + NOT_DELETED + " }", value).firstResultOptional();
  }

  /**
   * Checks whether a token value cannot be used for a new token. This includes values of deleted
   * tokens whose deletion event is pending, as that event would otherwise affect the new token.
   *
   * @param value the token value
   * @return whether the value is in use
   */
  public boolean isValueInUse(final String value) {
    return this.count("value", value) > 0;
  }

  public Collection<LandscapeToken> findForUser(final String userId) {
    return this.list("{ owner: ?1, " + NOT_DELETED + " }", userId);
  }

  public Collection<LandscapeToken> findSharedForUser(final String userId) {
    return this.list("{ sharedUsers: ?1, " + NOT_DELETED + " }", userId);
  }

  /**
   * Retrieve all landscape tokens.
   *
   * @return Collection of all landscape tokens
   */
  public Collection<LandscapeToken> getAll() {
    return this.list("{ " + NOT_DELETED + " }");
  }

  /**
   * Inserts a new token.
   *
   * @param token the token
   * @param events the resulting events
   */
  public void create(final LandscapeToken token, final PendingEvent... events) {
    final Document outbox =
        new Document("leaseUntil", 0L)
            .append("events", Arrays.stream(events).map(PendingEvent::toDocument).toList());
    final BsonDocument document =
        BsonDocumentWrapper.asBsonDocument(token, this.mongoCollection().getCodecRegistry());
    document.append(OUTBOX, outbox.toBsonDocument());
    this.mongoCollection().withDocumentClass(BsonDocument.class).insertOne(document);
  }

  /**
   * Grants a user access to a token.
   *
   * @param value the token value
   * @param userId the user
   * @return whether the user did not have access before
   */
  public boolean grantAccess(final String value, final String userId) {
    return this.change(
        Filters.and(existing(value), Filters.ne(SHARED_USERS, userId)),
        Updates.addToSet(SHARED_USERS, userId),
        PendingEvent.ofAccess(EventType.EVENT_TYPE_ACCESS_GRANTED, userId));
  }

  /**
   * Revokes a user's access to a token.
   *
   * @param value the token value
   * @param userId the user
   * @return whether the user had access before
   */
  public boolean revokeAccess(final String value, final String userId) {
    return this.change(
        Filters.and(existing(value), Filters.eq(SHARED_USERS, userId)),
        Updates.pull(SHARED_USERS, userId),
        PendingEvent.ofAccess(EventType.EVENT_TYPE_ACCESS_REVOKED, userId));
  }

  /**
   * Changes the alias of a token.
   *
   * @param value the token value
   * @param alias the new alias
   * @return whether the alias was different before
   */
  public boolean updateAlias(final String value, final String alias) {
    return this.change(
        Filters.and(existing(value), Filters.ne(ALIAS, alias)),
        Updates.set(ALIAS, alias),
        PendingEvent.of(EventType.EVENT_TYPE_UPDATED));
  }

  /**
   * Deletes a token. Its value cannot be reused until the deletion event is published.
   *
   * @param value the token value
   * @return whether the token existed before
   */
  public boolean markDeleted(final String value) {
    return this.change(
        existing(value), Updates.set(DELETED, true), PendingEvent.of(EventType.EVENT_TYPE_DELETED));
  }

  private boolean change(final Bson filter, final Bson update, final PendingEvent event) {
    return this.documents()
            .updateMany(
                filter,
                Updates.combine(
                    update,
                    Updates.push(EVENTS, event.toDocument()),
                    // Make events available for publishing, but do not shorten an existing lease
                    Updates.max(LEASE_UNTIL, 0L)))
            .getModifiedCount()
        > 0;
  }

  /**
   * Claims a token with pending events that nobody else is publishing.
   *
   * @param value the value of the token to be claimed, or {@code null} for any token
   * @param now the current time
   * @param leaseUntil the time until which others cannot claim the token
   * @return the claimed token
   */
  public Optional<PendingToken> claimPending(
      final String value, final long now, final long leaseUntil) {
    final Bson available = Filters.lte(LEASE_UNTIL, now);
    return this.lease(
        value == null ? available : Filters.and(available, Filters.eq(VALUE, value)), leaseUntil);
  }

  /**
   * Extends the lease of a claimed token.
   *
   * @param id the database id of the token
   * @param leaseUntil the time until which others cannot claim the token
   * @return the current state of the claimed token
   */
  public Optional<PendingToken> extendLease(final Object id, final long leaseUntil) {
    return this.lease(Filters.and(Filters.eq(ID, id), Filters.exists(LEASE_UNTIL)), leaseUntil);
  }

  private Optional<PendingToken> lease(final Bson filter, final long leaseUntil) {
    final Document document =
        this.documents()
            .findOneAndUpdate(
                filter,
                Updates.set(LEASE_UNTIL, leaseUntil),
                new FindOneAndUpdateOptions()
                    .sort(Sorts.ascending(LEASE_UNTIL))
                    .returnDocument(ReturnDocument.AFTER));
    return Optional.ofNullable(document).map(this::toPendingToken);
  }

  private PendingToken toPendingToken(final Document document) {
    final CodecRegistry codecs = this.mongoCollection().getCodecRegistry();
    final LandscapeToken token =
        codecs
            .get(LandscapeToken.class)
            .decode(
                new BsonDocumentReader(document.toBsonDocument(Document.class, codecs)),
                DecoderContext.builder().build());
    final List<PendingEvent> events =
        document.get(OUTBOX, Document.class).getList("events", Document.class).stream()
            .map(PendingEvent::fromDocument)
            .toList();
    return new PendingToken(document.get(ID), token, events);
  }

  /**
   * Removes a published event of a claimed token.
   *
   * @param id the database id of the token
   * @param event the published event
   */
  public void removeEvent(final Object id, final PendingEvent event) {
    this.documents()
        .updateOne(Filters.eq(ID, id), Updates.pull(EVENTS, new Document("id", event.id())));
  }

  /**
   * Ends the claim of a token without pending events.
   *
   * @param id the database id of the token
   * @return {@code false} if there are pending events, i.e. the token is still claimed
   */
  public boolean finishPublishing(final Object id) {
    return this.documents()
            .updateOne(
                Filters.and(Filters.eq(ID, id), Filters.size(EVENTS, 0)), Updates.unset(OUTBOX))
            .getMatchedCount()
        > 0;
  }

  /**
   * Removes the document of a token whose deletion was published.
   *
   * @param id the database id of the token
   */
  public void removeDeleted(final Object id) {
    this.documents().deleteOne(Filters.and(Filters.eq(ID, id), Filters.eq(DELETED, true)));
  }

  private static Bson existing(final String value) {
    return Filters.and(Filters.eq(VALUE, value), Filters.ne(DELETED, true));
  }

  private MongoCollection<Document> documents() {
    return this.mongoCollection().withDocumentClass(Document.class);
  }
}
