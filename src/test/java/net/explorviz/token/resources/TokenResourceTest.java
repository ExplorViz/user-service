package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.explorviz.token.TestUtils;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TokenResourceTest {

  @Test
  void getTokenByValue() {
    LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
    QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    final String uid = "testuid";
    final String value = "token";
    mockRepo.persist(new LandscapeToken(value, uid));
    given()
        .when().get("token/" + value)
        .then()
        .statusCode(200)
        .body("ownerId", is(uid))
        .body("value", is(value));
  }

  @Test
  void getTokenByUnknownValue() {
    LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
    QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    given()
        .when().get("token/unknown")
        .then()
        .statusCode(404);
  }

  @Test
  void deleteToken() {
    LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
    QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    final String uid = "testuid";
    final String value = "token";
    mockRepo.persist(new LandscapeToken(value, uid));
    given()
        .when().delete("token/" + value)
        .then()
        .statusCode(204);
    given()
        .when().get("token/"+value)
        .then()
        .statusCode(404);
  }

  @Test
  void deleteUnknownToken() {
    LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
    QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    given()
        .when().delete("token/unknown")
        .then()
        .statusCode(404);
  }
}
