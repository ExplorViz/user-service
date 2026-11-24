package net.explorviz.token;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.explorviz.persistence.DatabaseIndexes;
import net.explorviz.persistence.ExpiryJob;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.persistence.PendingEvent;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import net.explorviz.userapi.service.UserApiService;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class DatabaseIndexesIntegrationTest {

  @Inject DatabaseIndexes indexes;
  @Inject ExpiryJob expiryJob;
  @Inject LandscapeTokenRepository tokens;
  @Inject SnapshotRepository snapshots;
  @Inject UserApiRepository userApis;
  @Inject UserApiService userApiService;

  @AfterEach
  void tearDown() {
    this.tokens.mongoCollection().drop();
    this.snapshots.mongoCollection().drop();
    this.userApis.mongoCollection().drop();
  }

  private static Snapshot snapshot(final long createdAt, final String name, final long deleteAt) {
    return new Snapshot(
        "owner", createdAt, name, null, null, null, null, null, false, null, deleteAt);
  }

  private static boolean isUnique(final MongoCollection<?> collection, final String index) {
    for (final Document d : collection.listIndexes()) {
      if (index.equals(d.getString("name"))) {
        return d.getBoolean("unique", false);
      }
    }
    return false;
  }

  @Test
  void duplicatesAreRejected() {
    this.indexes.create();
    this.indexes.create(); // may be repeated

    final LandscapeToken token = new LandscapeToken("v", "s", "o", 1L, "a");
    this.tokens.create(token, new PendingEvent[0]);
    assertThrows(MongoWriteException.class, () -> this.tokens.create(token, new PendingEvent[0]));
    this.snapshots.persist(snapshot(1L, "a", 0L));
    assertThrows(MongoWriteException.class, () -> this.snapshots.persist(snapshot(1L, "b", 0L)));
    this.userApis.persist(new UserApi("u", "n", "t", "h", 1L, 0L));
    assertThrows(
        MongoWriteException.class,
        () -> this.userApis.persist(new UserApi("u", "n2", "t", "h", 2L, 0L)));
  }

  @Test
  void concurrentRequestsCreateOneEntry() throws Exception {
    this.indexes.create();
    final Callable<Integer> request =
        () ->
            given()
                .contentType("application/json")
                .body("{\"value\": \"same\"}")
                .post("user/u/token")
                .statusCode();
    try (var executor = Executors.newFixedThreadPool(8)) {
      final List<Future<Integer>> responses =
          executor.invokeAll(IntStream.range(0, 8).mapToObj(i -> request).toList());
      final var byStatus =
          responses.stream()
              .map(Future::resultNow)
              .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
      assertEquals(1L, byStatus.get(200), byStatus.toString());
      assertEquals(7L, byStatus.get(409), byStatus.toString());
    }
    assertEquals(1, this.tokens.count());
  }

  @Test
  void conflictsHaveMessage() {
    this.indexes.create();
    final String body = "{\"owner\": \"o\", \"createdAt\": 1}";
    given()
        .contentType("application/json")
        .body(body)
        .post("snapshot/create")
        .then()
        .statusCode(200);
    given()
        .contentType("application/json")
        .body(body)
        .post("snapshot/create")
        .then()
        .statusCode(409)
        .body("message", notNullValue());
  }

  @Test
  void redundantDocumentsAreRemoved() {
    // Stored again on every start by former versions
    this.userApis.persist(new UserApi("u", "n", "t", "h", 1L, 0L));
    this.userApis.persist(new UserApi("u", "n", "t", "h", 2L, 0L));
    this.userApis.persist(new UserApi("u", "n", "t2", "h", 2L, 0L));
    this.snapshots.persist(snapshot(1L, "same", 0L));
    this.snapshots.persist(snapshot(1L, "same", 0L));

    this.indexes.create();

    assertEquals(2, this.userApis.count());
    assertEquals(1L, this.userApis.findForUserAndToken("u", "t").iterator().next().getCreatedAt());
    assertEquals(1, this.snapshots.count());
    assertTrue(isUnique(this.userApis.mongoCollection(), "uid_1_token_1"));
    assertTrue(isUnique(this.snapshots.mongoCollection(), "owner_1_createdAt_1_isShared_1"));
  }

  @Test
  void conflictingDocumentsAreKept() {
    this.snapshots.persist(snapshot(1L, "a", 0L));
    this.snapshots.persist(snapshot(1L, "b", 0L));

    this.indexes.create();

    assertEquals(2, this.snapshots.count());
    assertFalse(isUnique(this.snapshots.mongoCollection(), "owner_1_createdAt_1_isShared_1"));
    assertTrue(isUnique(this.tokens.mongoCollection(), "value_1"));
    assertTrue(isUnique(this.userApis.mongoCollection(), "uid_1_token_1"));
  }

  @Test
  void onlyExpiredEntriesAreDeleted() {
    final long future = System.currentTimeMillis() + 60_000;
    this.snapshots.persist(snapshot(1L, "expired", 1L));
    this.snapshots.persist(snapshot(2L, "never", 0L));
    this.snapshots.persist(snapshot(3L, "later", future));
    this.snapshots.persist(
        new Snapshot("o", 4L, "null", null, null, null, null, null, false, null, null));
    this.userApis.persist(new UserApi("u", "n", "expired", "h", 1L, 1L));
    this.userApis.persist(new UserApi("u", "n", "never", "h", 1L, 0L));
    this.userApis.persist(new UserApi("u", "n", "later", "h", 1L, future));
    this.userApis.persist(new UserApi("u", "n", "null", "h", 1L, null));

    this.expiryJob.deleteExpired();

    assertEquals(
        List.of(2L, 3L, 4L),
        this.snapshots.listAll().stream().map(Snapshot::getCreatedAt).sorted().toList());
    assertEquals(
        List.of("later", "never", "null"),
        this.userApis.listAll().stream().map(UserApi::getToken).sorted().toList());
  }

  @Test
  void expiredApiTokenCanBeReplaced() {
    this.indexes.create();
    this.userApis.persist(new UserApi("u", "old", "t", "h", 1L, 1L));
    given()
        .queryParams("uId", "u", "token", "t", "name", "new")
        .post("userapi/create")
        .then()
        .statusCode(200);
    assertEquals(
        List.of("new"),
        this.userApis.findForUserAndToken("u", "t").stream().map(UserApi::getName).toList());
  }
}
