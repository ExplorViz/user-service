package net.explorviz.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.snapshot.service.SnapshotService;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class SnapshotRepositoryIntegrationTest {

  private static final String OWNER = "owner";
  private static final long CREATED_AT = 42L;

  @Inject SnapshotRepository repository;
  @Inject SnapshotService snapshotService;

  @AfterEach
  void tearDown() {
    this.repository.deleteAll();
  }

  private static Snapshot snapshot(final boolean isShared, final Document subscribers) {
    return new Snapshot(
        OWNER,
        CREATED_AT,
        "name",
        new Document(),
        new Document(),
        new Document(),
        new Document(),
        new Document(),
        isShared,
        subscribers,
        0L);
  }

  private List<String> subscribers() {
    final Snapshot sn = this.snapshotService.getSnapshot(OWNER, CREATED_AT, true);
    assertNotNull(sn);
    return sn.getSubscribedUsers().getList("subscriberList", String.class);
  }

  @Test
  void subscribeAndUnsubscribeWithoutSubscriberDocument() {
    this.repository.persist(snapshot(true, null));

    this.snapshotService.addNewSubscriber(OWNER, CREATED_AT, "a");
    this.snapshotService.addNewSubscriber(OWNER, CREATED_AT, "b");
    this.snapshotService.addNewSubscriber(OWNER, CREATED_AT, "a");
    assertEquals(List.of("a", "b"), subscribers());

    this.snapshotService.removeSubscriber(OWNER, CREATED_AT, "a");
    assertEquals(List.of("b"), subscribers());
    assertEquals(1, this.repository.count());
  }

  @Test
  void subscribeDoesNotTouchPersonalSnapshot() {
    this.repository.persist(snapshot(false, new Document()));
    this.snapshotService.addNewSubscriber(OWNER, CREATED_AT, "a");
    assertTrue(this.repository.findVisibleTo("a").isEmpty());
  }

  @Test
  void visibleSnapshotsIncludeOwnedAndSubscribed() {
    this.repository.persist(snapshot(false, null));
    this.repository.persist(snapshot(true, new Document("subscriberList", List.of("a"))));
    this.repository.persist(
        new Snapshot("other", 1L, "n", null, null, null, null, null, false, null, 0L));

    assertEquals(2, this.repository.findVisibleTo(OWNER).size());
    assertEquals(1, this.repository.findVisibleTo("a").size());
  }

  @Test
  void duplicatesCanBeReadAndDeleted() {
    this.repository.persist(snapshot(false, null));
    this.repository.persist(snapshot(false, null));

    assertNotNull(this.snapshotService.getSnapshot(OWNER, CREATED_AT, false));
    assertEquals(0, this.snapshotService.deleteByValue(OWNER, CREATED_AT, false));
    assertEquals(0, this.repository.count());
  }
}
