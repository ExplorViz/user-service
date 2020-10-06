package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import net.explorviz.token.TestUtils;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TokenResourceTest {

  @QuarkusTest
  static class TokenGeneration {

    @BeforeAll
    static void beforeAll() {
      LandscapeTokenRepository mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    }

    @Test
    public void testTokenCreationEndpoint() {
      final String sampleUid = "testuid";
      given()
          .when().post("/"+sampleUid+"/token")
          .then()
          .statusCode(200)
          .body("ownerId", equalTo(sampleUid))
          .body("value", CoreMatchers.notNullValue())
          .body("value", CoreMatchers.isA(String.class));
    }
  }

}
