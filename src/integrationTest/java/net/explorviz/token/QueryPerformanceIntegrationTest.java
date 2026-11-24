package net.explorviz.token;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import net.explorviz.persistence.DatabaseIndexes;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks how MongoDB executes the queries of requests, according to its profiler. */
@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class QueryPerformanceIntegrationTest {

  private static final int USERS = 5;
  private static final int ENTRIES = 20;
  private static final int STRUCTURE_BYTES = 10_000;
  private static final String USER = "u1";

  @Inject DatabaseIndexes indexes;
  @Inject LandscapeTokenRepository tokens;
  @Inject SnapshotRepository snapshots;
  @Inject UserApiRepository userApis;

  @BeforeEach
  void setUp() {
    this.indexes.create();
    for (int user = 0; user < USERS; user++) {
      final String owner = "u" + user;
      final String other = "u" + (user + 1) % USERS;
      for (int i = 0; i < ENTRIES; i++) {
        final String value = owner + "-" + i;
        this.tokens.persist(new LandscapeToken(value, "s", owner, i, "a", List.of(other, "x")));
        this.userApis.persist(new UserApi(owner, "n", value, "h", (long) i, 0L));
        this.snapshots.persist(snapshot(owner, i, false, List.of()));
        this.snapshots.persist(snapshot(owner, i, true, List.of(other, "x")));
      }
    }
  }

  @AfterEach
  void tearDown() {
    this.database().runCommand(new Document("profile", 0));
    this.database().getCollection("system.profile").drop();
    this.tokens.mongoCollection().drop();
    this.snapshots.mongoCollection().drop();
    this.userApis.mongoCollection().drop();
  }

  private static Snapshot snapshot(
      final String owner, final long createdAt, final boolean shared, final List<String> users) {
    final Document data = new Document("data", "x".repeat(STRUCTURE_BYTES));
    return new Snapshot(
        owner,
        createdAt,
        "n",
        new Document("value", "v"),
        data,
        null,
        null,
        null,
        shared,
        new Document("subscriberList", users),
        0L);
  }

  private MongoDatabase database() {
    return this.snapshots.mongoDatabase();
  }

  /** Executes a request and returns the resulting queries of the given collection. */
  private List<Document> queriesOf(
      final PanacheMongoRepositoryBase<?, ?> repository, final Runnable request) {
    this.database().runCommand(new Document("profile", 2));
    request.run();
    this.database().runCommand(new Document("profile", 0));
    final List<Document> queries =
        this.database()
            .getCollection("system.profile")
            .find(Filters.eq("ns", repository.mongoCollection().getNamespace().getFullName()))
            .sort(new Document("ts", 1))
            .into(new ArrayList<>());
    assertFalse(queries.isEmpty());
    for (final Document query : queries) {
      if (!"insert".equals(query.getString("op"))) {
        final String plan = query.getString("planSummary");
        assertTrue(plan.contains("IXSCAN") || plan.contains("COUNT_SCAN"), query.toJson());
        assertFalse(query.getBoolean("hasSortStage", false), query.toJson());
      }
    }
    return queries;
  }

  private static void get(final String path) {
    given().get(path).then().statusCode(200);
  }

  @Test
  void snapshotListReadsOnlyItsPage() {
    final List<Document> queries =
        this.queriesOf(this.snapshots, () -> get("snapshot?owner=" + USER + "&size=5&page=1"));

    assertEquals(3, queries.size());
    for (final Document query : queries) {
      assertEquals(5, query.getInteger("nreturned"), query.toJson());
      // Skipped snapshots may have to be read, all others must not be
      assertTrue(query.getInteger("docsExamined") <= 10, query.toJson());
      assertTrue(query.getInteger("responseLength") < STRUCTURE_BYTES, query.toJson());
    }
  }

  @Test
  void completeSnapshotListDoesNotContainSnapshotData() {
    final List<Document> queries =
        this.queriesOf(this.snapshots, () -> get("snapshot?owner=" + USER));

    assertEquals(3, queries.size());
    for (final Document query : queries) {
      assertEquals(ENTRIES, query.getInteger("nreturned"), query.toJson());
      assertEquals(ENTRIES, query.getInteger("docsExamined"), query.toJson());
      assertTrue(query.getInteger("responseLength") < STRUCTURE_BYTES, query.toJson());
    }
  }

  @Test
  void existenceChecksDoNotReturnDocuments() {
    final Document created = new Document("owner", USER).append("createdAt", 1L);
    final List<Document> snapshotQueries =
        this.queriesOf(
            this.snapshots,
            () -> {
              given()
                  .contentType("application/json")
                  .body(created.toJson())
                  .post("snapshot/create")
                  .then()
                  .statusCode(409);
              given().put("snapshot/share?owner=" + USER + "&createdAt=1").then().statusCode(222);
              given()
                  .delete("snapshot/delete?owner=" + USER + "&createdAt=2&isShared=true")
                  .then()
                  .statusCode(200);
            });
    assertEquals(3, snapshotQueries.size());
    snapshotQueries.forEach(q -> assertTrue(q.getInteger("responseLength", 0) < 1000, q.toJson()));

    final List<Document> userApiQueries =
        this.queriesOf(
            this.userApis,
            () -> {
              given().post("userapi/create?uId=" + USER + "&token=u1-1").then().statusCode(409);
              given().delete("userapi/delete?uId=" + USER + "&token=u1-2").then().statusCode(200);
            });
    assertEquals(2, userApiQueries.size());
    assertEquals(
        0,
        userApiQueries.stream()
            .filter(
                q ->
                    q.containsKey("nreturned")
                        && q.get("command", Document.class).containsKey("find"))
            .count());
  }

  @Test
  void userApiListIsQueriedOnce() {
    final List<Document> queries = this.queriesOf(this.userApis, () -> get("userapi?uId=" + USER));
    assertEquals(1, queries.size());
    assertEquals(ENTRIES, queries.getFirst().getInteger("docsExamined"));
  }

  @Test
  void tokensAreFoundByIndex() {
    final List<Document> queries =
        this.queriesOf(
            this.tokens,
            () -> {
              get("user/" + USER + "/token");
              this.tokens.findByValue("u1-1").orElseThrow();
              assertTrue(this.tokens.isValueInUse("u1-1"));
              assertEquals(ENTRIES, this.tokens.findSharedForUser(USER).size());
            });
    queries.forEach(q -> assertTrue(q.getInteger("docsExamined", 0) <= ENTRIES, q.toJson()));
  }
}
