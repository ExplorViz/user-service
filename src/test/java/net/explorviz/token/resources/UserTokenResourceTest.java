package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class UserTokenResourceTest {

  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;


  @BeforeEach
  void setUp() {


    repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(repo, LandscapeTokenRepository.class);
    EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
    QuarkusMock.installMockForType(mockEventService, EventService.class);


    inMemRepo = new InMemRepo();
    Mockito.doAnswer(invocation -> {
      inMemRepo.addToken(invocation.getArgument(0));
      return null;
    }).when(repo).persist(Mockito.any(LandscapeToken.class));

    Mockito.when(repo.findForUser(Mockito.anyString())).thenAnswer(
        invocation -> inMemRepo.findForUser(invocation.getArgument(0)));



  }

  @Test
  public void testTokenCreationEndpoint() {
    final String sampleUid = "testuid";
    given()
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
    repo.persist(new LandscapeToken(value, uid));
    given()
        .when().get("user/" + uid + "/token")
        .then()
        .statusCode(200)
        .body("size()", is(1))
        .body("[0].ownerId", is(uid))
        .body("[0].value", is(value));
  }

  @Test
  public void testTokenRetrieveMultiple() {

    final String uid = "testuid";
    final int tokens = 100;
    for (int i = 0; i < tokens; i++) {
      repo.persist(new LandscapeToken(String.valueOf(i), uid));
      repo.persist(new LandscapeToken(String.valueOf(i), "other"));
    }
    given()
        .when().get("user/" + uid + "/token")
        .then()
        .statusCode(200)
        .body("size()", is(tokens));
  }



}
