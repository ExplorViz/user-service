package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import javax.ws.rs.core.MediaType;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@QuarkusTest
class UserTokenResourceTest {

  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;

  private static final String SECRET = "secret";


  @BeforeEach
  void setUp() {

    this.repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(this.repo, LandscapeTokenRepository.class);
    final EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
    QuarkusMock.installMockForType(mockEventService, EventService.class);

    this.inMemRepo = new InMemRepo();
    Mockito.doAnswer(invocation -> {
      this.inMemRepo.addToken(invocation.getArgument(0));
      return null;
    }).when(this.repo).persist(ArgumentMatchers.any(LandscapeToken.class));

    Mockito.when(this.repo.findForUser(ArgumentMatchers.anyString())).thenAnswer(
        invocation -> this.inMemRepo.findForUser(invocation.getArgument(0)));


  }

  @Test
  public void testTokenCreationEndpoint() {
    final String sampleUid = "testuid";
    given().contentType(MediaType.APPLICATION_JSON)
        .when().post("user/" + sampleUid + "/token/")
        .then()
        .statusCode(200)
        .body("ownerId", equalTo(sampleUid))
        .body("value", CoreMatchers.notNullValue())
        .body("value", CoreMatchers.isA(String.class));
  }


  @Test
  public void testTokenRetrieveEmpty() {

    final String sampleUid = "testuid";
    given()
        .when().get("user/" + sampleUid + "/token/")
        .then()
        .statusCode(200)
        .body("size()", is(0));
  }

  @Test
  public void testTokenRetrieve() {

    final String uid = "testuid";
    final String value = "token";
    final long created = System.currentTimeMillis();
    final String alias = "somealias";
    this.repo.persist(new LandscapeToken(value, SECRET, uid, created, alias));
    given()
        .when().get("user/" + uid + "/token")
        .then()
        .statusCode(200)
        .body("size()", is(1))
        .body("[0].ownerId", is(uid))
        .body("[0].value", is(value))
        .body("[0].created", is(created))
        .body("[0].alias", is(alias));
  }

  @Test
  public void testTokenRetrieveMultiple() {

    final String uid = "testuid";
    final long created = System.currentTimeMillis();
    final String alias = "somealias";
    final int tokens = 100;
    for (int i = 0; i < tokens; i++) {
      this.repo.persist(new LandscapeToken(String.valueOf(i), SECRET, uid, created, alias));
      this.repo.persist(new LandscapeToken(String.valueOf(i), SECRET, "other", created, alias));
    }
    given()
        .when().get("user/" + uid + "/token")
        .then()
        .statusCode(200)
        .body("size()", is(tokens));
  }


}
