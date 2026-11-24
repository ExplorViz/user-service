package net.explorviz.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.panache.common.Page;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.model.SnapshotOverview;
import net.explorviz.snapshot.model.SnapshotSummary;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.snapshot.service.SnapshotService;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class SnapshotRepositoryIntegrationTest {

  private static final String OWNER = "owner";
  private static final long CREATED_AT = 42L;
  private static final Page ALL = Page.ofSize(Integer.MAX_VALUE);

  @Inject SnapshotRepository repository;
  @Inject UserApiRepository userApis;
  @Inject SnapshotService snapshotService;

  @AfterEach
  void tearDown() {
    this.repository.deleteAll();
    this.userApis.deleteAll();
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

  private SnapshotOverview visible(final String user, final Page page) {
    return this.snapshotService.getVisibleSnapshots(user, page);
  }

  private static List<Long> dates(final List<SnapshotSummary> summaries) {
    return summaries.stream().map(SnapshotSummary::createdAt).toList();
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
    assertTrue(visible("a", ALL).subscribedSnapshots().isEmpty());
  }

  @Test
  void visibleSnapshotsIncludeOwnedAndSubscribed() {
    this.repository.persist(snapshot(false, null));
    this.repository.persist(snapshot(true, new Document("subscriberList", List.of("a"))));
    this.repository.persist(
        new Snapshot("other", 1L, "n", null, null, null, null, null, false, null, 0L));

    assertEquals(1, visible(OWNER, ALL).personalSnapshots().size());
    assertEquals(1, visible(OWNER, ALL).sharedSnapshots().size());
    assertEquals(0, visible(OWNER, ALL).subscribedSnapshots().size());
    assertEquals(0, visible("a", ALL).personalSnapshots().size());
    assertEquals(0, visible("a", ALL).sharedSnapshots().size());
    assertEquals(1, visible("a", ALL).subscribedSnapshots().size());
  }

  @Test
  void ownersAreNotListedAsSubscribers() {
    this.repository.persist(snapshot(true, new Document("subscriberList", List.of(OWNER))));
    assertEquals(1, visible(OWNER, ALL).sharedSnapshots().size());
    assertEquals(0, visible(OWNER, ALL).subscribedSnapshots().size());
  }

  @Test
  void summariesContainStoredValues() {
    final Document token = new Document("value", "v").append("alias", "a");
    this.repository.persist(
        new Snapshot(OWNER, CREATED_AT, "n", token, null, null, null, null, false, null, 0L));
    // Stored by something else, with fewer fields and another number type
    this.repository
        .mongoCollection()
        .withDocumentClass(Document.class)
        .insertOne(new Document("owner", OWNER).append("createdAt", 7));

    assertEquals(
        List.of(
            new SnapshotSummary(OWNER, CREATED_AT, "n", token),
            new SnapshotSummary(OWNER, 7L, null, null)),
        visible(OWNER, ALL).personalSnapshots());
  }

  @Test
  void expiredSnapshotsAreNotListed() {
    final long now = System.currentTimeMillis();
    final Document subscribers = new Document("subscriberList", List.of("a"));
    final Long[] deleteAt = {1L, -1L, 0L, null, now + 60_000};
    for (int i = 0; i < deleteAt.length; i++) {
      for (final boolean shared : new boolean[] {true, false}) {
        final Snapshot sn =
            new Snapshot(
                OWNER,
                (long) i,
                "n",
                null,
                null,
                null,
                null,
                null,
                shared,
                subscribers,
                deleteAt[i]);
        this.repository.persist(sn);
        if (shared) {
          assertEquals(
              !Snapshot.isExpired(sn, now), this.repository.isShared(OWNER, (long) i, now));
        }
      }
    }
    final List<Long> remaining = List.of(4L, 3L, 2L);
    assertEquals(remaining, dates(visible(OWNER, ALL).personalSnapshots()));
    assertEquals(remaining, dates(visible(OWNER, ALL).sharedSnapshots()));
    assertEquals(List.of(4L, 4L, 3L, 3L, 2L, 2L), dates(visible("a", ALL).subscribedSnapshots()));
    // Expired snapshots do not take up space on a page
    assertEquals(List.of(2L), dates(visible(OWNER, Page.of(1, 2)).personalSnapshots()));
  }

  @Test
  void expiredApiTokensDoNotExist() {
    final long now = System.currentTimeMillis();
    final Long[] expires = {1L, -1L, 0L, null, now + 60_000};
    for (int i = 0; i < expires.length; i++) {
      final UserApi userApi = new UserApi(OWNER, "n", "t" + i, "h", 1L, expires[i]);
      this.userApis.persist(userApi);
      assertEquals(!UserApi.isExpired(userApi, now), this.userApis.exists(OWNER, "t" + i, now));
    }
    assertFalse(this.userApis.exists("other", "t2", now));
  }

  @Test
  void expiredSharedSnapshotIsReplaced() {
    this.repository.persist(snapshot(false, null));
    this.repository.persist(
        new Snapshot(OWNER, CREATED_AT, "old", null, null, null, null, null, true, null, 1L));

    assertEquals(0, this.snapshotService.shareSnapshot(OWNER, CREATED_AT, 0L));
    assertEquals(1, this.snapshotService.shareSnapshot(OWNER, CREATED_AT, 0L));
    assertEquals(-1, this.snapshotService.shareSnapshot(OWNER, 1L, 0L));
    assertEquals("name", this.snapshotService.getSnapshot(OWNER, CREATED_AT, true).getName());
    assertEquals(2, this.repository.count());
  }

  @Test
  void pagesOfSubscribedSnapshotsAreComplete() {
    final Document subscribers = new Document("subscriberList", List.of("x", "a"));
    for (int i = 0; i < 5; i++) {
      // Same date for all owners
      this.repository.persist(
          new Snapshot("o" + i, 1L, "n", null, null, null, null, null, true, subscribers, 0L));
    }
    final List<String> owners = new ArrayList<>();
    for (int page = 0; page < 3; page++) {
      visible("a", Page.of(page, 2)).subscribedSnapshots().forEach(s -> owners.add(s.owner()));
    }
    assertEquals(List.of("o4", "o3", "o2", "o1", "o0"), owners);
    assertTrue(visible("a", Page.of(3, 2)).subscribedSnapshots().isEmpty());
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
