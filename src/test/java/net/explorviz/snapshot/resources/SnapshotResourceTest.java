package net.explorviz.snapshot.resources;

import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.snapshot.service.SnapshotInMemRepo;
import net.explorviz.snapshot.service.SnapshotServiceImpl;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class SnapshotResourceTest {

  SnapshotRepository repo;
  SnapshotInMemRepo inMemRepo;
  SnapshotServiceImpl snapshotService;

  @BeforeEach
  void setUp() {

    this.repo = Mockito.mock(SnapshotRepository.class);
    QuarkusMock.installMockForType(this.repo, SnapshotRepository.class);

    this.inMemRepo = new SnapshotInMemRepo();
    Mockito.doAnswer(invocation -> {
      this.inMemRepo.addSnapshot(invocation.getArgument(0));
      return null;
    }).when(this.repo).persist(ArgumentMatchers.any(Snapshot.class));

    Mockito.when(this.repo.findForUser(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findForUser(invocation.getArgument(0)));

    Mockito.when(this.repo.findForUserAndCreatedAtAndIsShared(
        ArgumentMatchers.anyString(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyBoolean()
    )).thenAnswer(invocation -> this.inMemRepo.findForUserAndCreatedAtAndIsShared(
        invocation.getArgument(0),
        invocation.getArgument(1),
        invocation.getArgument(2)
    ));

    Mockito.when(this.repo.getAll()).thenAnswer(invocation -> this.inMemRepo.getAll());
  }

  @Test
  public void testSnapshotCreationEndpoint() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.snapshotService = Mockito.mock(SnapshotServiceImpl.class);
    QuarkusMock.installMockForType(this.snapshotService, SnapshotServiceImpl.class);
    Mockito.when(this.snapshotService.snapshotExists(ArgumentMatchers.anyString(),
        ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean()))
        .thenAnswer(invocation -> false);

    given().body(snapshot).contentType(MediaType.APPLICATION_JSON).when().post("snapshot/create/")
        .then().statusCode(200);
  }

  @Test
  public void testSnapshotCreationOfExistingEndpoint() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().body(snapshot).contentType(MediaType.APPLICATION_JSON).when().post("snapshot/create/")
        .then().statusCode(422);
  }

  @Test
  public void testSnapshotRetrieveAllByOwnerEmpty() {
    given().when().get("snapshot/").then().statusCode(200)
        .body("size()", is(3));
  }

  @Test
  public void testSnapshotRetrieveAllByOwnerPersonal() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().param("owner", owner).when().get("snapshot/").then().statusCode(200)
        .body("size()", is(3))
        .body("personalSnapshots[0].owner", is(owner))
        .body("personalSnapshots[0].createdAt", is(createdAt))
        .body("personalSnapshots[0].name", is(name))
        .body("personalSnapshots[0].landscapeToken", is(landscapeToken));
  }

  @Test
  public void testSnapshotRetrieveAllByOwnerShared() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = true;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().param("owner", owner).when().get("snapshot/").then().statusCode(200)
        .body("size()", is(3))
        .body("sharedSnapshots[0].owner", is(owner))
        .body("sharedSnapshots[0].createdAt", is(createdAt))
        .body("sharedSnapshots[0].name", is(name))
        .body("sharedSnapshots[0].landscapeToken", is(landscapeToken));
  }

  @Test
  public void testSnapshotRetrieveAllByOwnerSubscribed() {
    final String user = "testuser";

    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = System.currentTimeMillis();
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = true;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    ArrayList<String> subList = new ArrayList<>();
    subList.add(user);
    subscribedUsers.append("subscriberList", subList);

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().param("owner", user).when().get("snapshot/").then().statusCode(200)
        .body("size()", is(3))
        .body("subscribedSnapshots[0].owner", is(owner))
        .body("subscribedSnapshots[0].createdAt", is(createdAt))
        .body("subscribedSnapshots[0].name", is(name))
        .body("subscribedSnapshots[0].landscapeToken", is(landscapeToken));
  }

  @Test
  void testDeleteSnapshot() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = 1700000L;
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 0L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(),
        ArgumentMatchers.<Object>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(owner, createdAt, isShared));

    this.repo.persist(snapshot);

    given().params("owner", owner,
        "createdAt", createdAt,
        "isShared", isShared).when().delete("snapshot/delete/")
        .then().statusCode(200);
  }

  @Test
  void testDeleteNonExistingSnapshot() {
    final String owner = "testowner";
    final long createdAt = 1700000L;
    final boolean isShared = false;

    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(),
            ArgumentMatchers.<Object>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(owner, createdAt, isShared));

    given().params("owner", owner,
            "createdAt", createdAt,
            "isShared", isShared).when().delete("snapshot/delete/")
        .then().statusCode(400);
  }

  @Test
  void testSubscribeSnapshot() {
    final String owner = "testowner";
    final long createdAt = 1700000L;
    final String subscriber = "testsubscriber";

    // doesn't need more tests, because 200 should always be the
    // response (frontend doesn't need any infos)
    given().params("owner", owner,
            "createdAt", createdAt,
            "subscriber", subscriber).when().put("snapshot/subscribe/")
        .then().statusCode(200);
  }

  @Test
  void testUnsubscribeSnapshot() {
    final String owner = "testowner";
    final long createdAt = 1700000L;
    final String subscriber = "testsubscriber";

    // doesn't need more tests, because 200 should always be the
    // response (frontend doesn't need any infos)
    given().params("owner", owner,
            "createdAt", createdAt,
            "subscriber", subscriber).when().put("snapshot/unsubscribe/")
        .then().statusCode(200);
  }

  @Test
  void testShareSnapshot() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = 1700000L;
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = false;
    final Document subscribedUsers = new Document();
    final long deleteAt = 1900000000L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().params("owner", owner,
        "createdAt", createdAt,
        "deleteAt", deleteAt).when().put("snapshot/share/")
        .then().statusCode(200);
  }

  @Test
  void testShareAlreadySharedSnapshot() {
    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = 1700000L;
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = true;
    final Document subscribedUsers = new Document();
    final long deleteAt = 1900000000L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().params("owner", owner,
            "createdAt", createdAt,
            "deleteAt", deleteAt).when().put("snapshot/share/")
        .then().statusCode(222);
  }

  @Test
  void testShareNonExistingSnapshot() {
    final String owner = "testowner";
    final long createdAt = 1700000L;
    final long deleteAt = 1900000000L;

    given().params("owner", owner,
            "createdAt", createdAt,
            "deleteAt", deleteAt).when().put("snapshot/share/")
        .then().statusCode(400);
  }

  @Test
  void testGetSpecificSnapshot() {
    final String subscriber = "testowner";

    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = 1700000L;
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = true;
    final Document subscribedUsers = new Document();
    final long deleteAt = 1900000000L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    given().params("owner", owner,
        "createdAt", createdAt,
        "isShared", isShared,
        "subscriber", subscriber).when().get("snapshot/get/")
        .then().statusCode(200).body("size()", is(11))
        .body("owner", is(owner))
        .body("createdAt", is(1700000))
        .body("name", is(name))
        .body("landscapeToken", is(landscapeToken))
        .body("structureData", is(structureData))
        .body("serializedRoom", is(serializedRoom))
        .body("timestamps", is(timestamps))
        .body("camera", is(camera))
        .body("isShared", is(isShared))
        .body("subscribedUsers", is(subscribedUsers))
        .body("deleteAt", is(1900000000));
  }

  @Test
  void testGetSpecificSharedSnapshot() {
    final String subscriber = "testsubscriber";

    final String owner = "testowner";
    final String name = "testname";
    final long createdAt = 1700000L;
    final Document landscapeToken = new Document();
    final Document structureData = new Document();
    final Document serializedRoom = new Document();
    final Document timestamps = new Document();
    final Document camera = new Document();
    final boolean isShared = true;
    final Document subscribedUsers = new Document();
    final long deleteAt = 1900000000L;

    final Snapshot snapshot = new Snapshot(owner, createdAt, name, landscapeToken,
        structureData, serializedRoom, timestamps, camera, isShared, subscribedUsers, deleteAt);

    this.repo.persist(snapshot);

    ArrayList<String> subList = new ArrayList<>();
    subList.add(subscriber);
    subscribedUsers.append("subscriberList", subList);

    given().params("owner", owner,
            "createdAt", createdAt,
            "isShared", isShared,
            "subscriber", subscriber).when().get("snapshot/get/")
        .then().statusCode(200).body("size()", is(11))
        .body("owner", is(owner))
        .body("createdAt", is(1700000))
        .body("name", is(name))
        .body("landscapeToken", is(landscapeToken))
        .body("structureData", is(structureData))
        .body("serializedRoom", is(serializedRoom))
        .body("timestamps", is(timestamps))
        .body("camera", is(camera))
        .body("isShared", is(isShared))
        .body("subscribedUsers", is(subscribedUsers))
        .body("deleteAt", is(1900000000));
  }
}
