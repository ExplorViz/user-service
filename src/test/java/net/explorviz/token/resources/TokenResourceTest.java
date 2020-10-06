package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.explorviz.token.TestUtils;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class TokenResourceTest {

  @QuarkusTest
  static class TokenCreation {

    @BeforeAll
    static void beforeAll() {
      LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    }

    @Test
    public void testTokenCreationEndpoint() {
      final String sampleUid = "testuid";
      given()
          .when().post("/" + sampleUid + "/token")
          .then()
          .statusCode(200)
          .body("ownerId", equalTo(sampleUid))
          .body("value", CoreMatchers.notNullValue())
          .body("value", CoreMatchers.isA(String.class));
    }
  }


  @QuarkusTest
  static class TokenRetrieval {

    @Test
    public void testTokenRetrieveEmpty() {
      LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
      final String sampleUid = "testuid";
      given()
          .when().get("/" + sampleUid + "/token")
          .then()
          .statusCode(200)
          .body("size()", is(0));
    }

    @Test
    public void testTokenRetrieve() {
      LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);

      final String uid = "testuid";
      final String value = "token";
      mockRepo.persist(new LandscapeToken(value, uid));
      given()
          .when().get("/" + uid + "/token")
          .then()
          .statusCode(200)
          .body("size()", is(1))
          .body("[0].ownerId", is(uid))
          .body("[0].value", is(value));
    }

    @Test
    public void testTokenRetrieveAll() {
      LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);

      final String uid = "testuid";
      final int tokens = 100;
      for (int i = 0; i < tokens; i++) {
        mockRepo.persist(new LandscapeToken(String.valueOf(i), uid));
        mockRepo.persist(new LandscapeToken(String.valueOf(i), "other"));
      }
      given()
          .when().get("/" + uid + "/token")
          .then()
          .statusCode(200)
          .body("size()", is(tokens));
    }

  }

}
