package net.explorviz.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.TokenService;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import net.explorviz.token.service.messaging.TokenEventOutbox;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class TokenEventOutboxIntegrationTest {

  private static final String OWNER = "owner";
  private static final String VALUE = "token-value";

  @Inject TokenService tokenService;
  @Inject TokenEventOutbox outbox;
  @Inject LandscapeTokenRepository tokens;
  @Inject SnapshotRepository snapshots;

  private final FakeEventService events = new FakeEventService();

  /** Records events instead of sending them to Kafka. */
  static class FakeEventService extends EventServiceImpl {
    final List<TokenEvent> sent = Collections.synchronizedList(new ArrayList<>());
    volatile boolean available = true;

    @Override
    public CompletionStage<Void> dispatch(final TokenEvent event) {
      if (!this.available) {
        return CompletableFuture.failedFuture(new IllegalStateException("Kafka not available"));
      }
      this.sent.add(event);
      return CompletableFuture.completedFuture(null);
    }

    List<EventType> types() {
      return List.copyOf(this.sent).stream().map(TokenEvent::getType).toList();
    }
  }

  @BeforeEach
  void setUp() {
    QuarkusMock.installMockForType(this.events, EventService.class);
  }

  @AfterEach
  void tearDown() {
    this.tokens.deleteAll();
    this.snapshots.deleteAll();
  }

  private MongoCollection<Document> documents() {
    return this.tokens.mongoCollection().withDocumentClass(Document.class);
  }

  private static void await(final BooleanSupplier condition) throws InterruptedException {
    for (int i = 0; i < 100 && !condition.getAsBoolean(); i++) {
      Thread.sleep(50);
    }
    assertTrue(condition.getAsBoolean());
  }

  private LandscapeToken create() {
    return this.tokenService.createNewToken(OWNER, "alias", VALUE, "secret");
  }

  @Test
  void allChangesArePublishedInOrder() throws Exception {
    final LandscapeToken token = create();
    this.tokenService.grantAccess(token, "friend");
    this.tokenService.grantAccess(token, "friend"); // no change
    this.tokenService.updateAlias(token, "new");
    this.tokenService.updateAlias(token, "new"); // no change
    this.tokenService.revokeAccess(token, "friend");
    this.tokenService.revokeAccess(token, "friend"); // no change
    awaitNothingPending();

    assertEquals(
        List.of(
            EventType.EVENT_TYPE_CREATED,
            EventType.EVENT_TYPE_ACCESS_GRANTED,
            EventType.EVENT_TYPE_UPDATED,
            EventType.EVENT_TYPE_ACCESS_REVOKED),
        this.events.types());
    assertEquals("friend", this.events.sent.get(1).getUserId());
    assertEquals("new", this.events.sent.get(3).getToken().getAlias());
    assertEquals("secret", this.events.sent.get(0).getToken().getSecret());
    assertEquals(
        new LandscapeToken(VALUE, "secret", OWNER, token.getCreated(), "new"),
        this.tokenService.getByValue(VALUE).orElseThrow());
  }

  private void awaitNothingPending() throws InterruptedException {
    await(
        () -> {
          this.outbox.publishAll();
          return this.documents().countDocuments(Filters.exists("outbox")) == 0;
        });
  }

  @Test
  void cloneIsPublishedAfterCreation() throws Exception {
    final LandscapeToken clone = this.tokenService.cloneToken("original", OWNER, "alias");
    awaitNothingPending();
    assertEquals(
        List.of(EventType.EVENT_TYPE_CREATED, EventType.EVENT_TYPE_CLONED), this.events.types());
    assertEquals("original", this.events.sent.get(1).getClonedTokenId());
    assertEquals(clone.getValue(), this.events.sent.get(1).getToken().getId());
  }

  @Test
  void failedEventsAreRetried() throws Exception {
    this.events.available = false;
    final LandscapeToken token = create();
    this.tokenService.grantAccess(token, "friend");
    // Wait for the failed attempt, which postpones the next one
    await(
        () ->
            this.documents()
                    .countDocuments(
                        Filters.and(
                            Filters.size("outbox.events", 2), Filters.gt("outbox.leaseUntil", 0L)))
                == 1);
    Thread.sleep(300);
    assertTrue(this.events.sent.isEmpty());

    this.events.available = true;
    this.documents()
        .updateMany(new Document(), new Document("$set", new Document("outbox.leaseUntil", 0L)));
    awaitNothingPending();
    assertEquals(
        List.of(EventType.EVENT_TYPE_CREATED, EventType.EVENT_TYPE_ACCESS_GRANTED),
        this.events.types());
  }

  @Test
  void claimedTokensAreNotPublishedByOthers() throws Exception {
    create();
    awaitNothingPending();
    this.tokens.grantAccess(VALUE, "a");
    final long now = System.currentTimeMillis();
    assertTrue(this.tokens.claimPending(VALUE, now, now + 60_000).isPresent());
    assertFalse(this.tokens.claimPending(VALUE, now, now + 60_000).isPresent());
    // Further changes do not break the lease
    this.tokens.grantAccess(VALUE, "b");
    assertFalse(this.tokens.claimPending(null, now, now + 60_000).isPresent());
    // An expired lease may be taken over
    assertTrue(this.tokens.claimPending(null, now + 60_000, now + 120_000).isPresent());
  }

  @Test
  void deletionIsPublishedEvenIfKafkaWasUnavailable() throws Exception {
    final LandscapeToken token = create();
    awaitNothingPending();
    this.events.sent.clear();
    this.events.available = false;
    this.tokenService.deleteByValue(token);

    assertTrue(this.tokenService.getByValue(VALUE).isEmpty());
    assertTrue(this.tokenService.getOwningTokens(OWNER).isEmpty());
    assertTrue(this.tokenService.getAllTokens().isEmpty());
    assertEquals(1, this.documents().countDocuments());
    // Otherwise, the deletion event would be published after the creation of the new token
    assertThrows(WebApplicationException.class, this::create);

    Thread.sleep(300);
    this.events.available = true;
    this.documents()
        .updateMany(new Document(), new Document("$set", new Document("outbox.leaseUntil", 0L)));
    this.outbox.publishAll();
    await(() -> this.documents().countDocuments() == 0);
    assertEquals(List.of(EventType.EVENT_TYPE_DELETED), this.events.types());

    // The value may be used again
    create();
    assertNull(this.documents().find().first().get("deleted"));
  }

  @Test
  void deletionRemovesSnapshotsOfOwnerOnly() throws Exception {
    this.snapshots.persist(snapshot(OWNER, 1L, VALUE, false));
    this.snapshots.persist(snapshot(OWNER, 1L, VALUE, true));
    this.snapshots.persist(snapshot(OWNER, 2L, "other-token", false));
    this.snapshots.persist(snapshot("friend", 3L, VALUE, false));
    this.tokenService.deleteByValue(create());

    assertEquals(
        List.of(2L, 3L),
        this.snapshots.listAll().stream().map(Snapshot::getCreatedAt).sorted().toList());
    awaitNothingPending();
    assertEquals(2, this.snapshots.count());
  }

  @Test
  void existingValueIsRejected() {
    create();
    final var e = assertThrows(WebApplicationException.class, this::create);
    assertEquals(409, e.getResponse().getStatus());
  }

  private static Snapshot snapshot(
      final String owner, final long createdAt, final String token, final boolean isShared) {
    return new Snapshot(
        owner,
        createdAt,
        "name",
        new Document("value", token),
        null,
        null,
        null,
        null,
        isShared,
        null,
        0L);
  }
}
