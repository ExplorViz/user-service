package net.explorviz.auth;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Authentication is disabled by default. */
@QuarkusTest
class AuthenticationDisabledTest {

  @BeforeEach
  void setUp() {
    final LandscapeTokenRepository repository = Mockito.mock(LandscapeTokenRepository.class);
    Mockito.when(repository.findForUser(Mockito.anyString())).thenReturn(new ArrayList<>());
    QuarkusMock.installMockForType(repository, LandscapeTokenRepository.class);
  }

  @Test
  void anonymousCallerIsAccepted() {
    given().get("/user/any-user/token").then().statusCode(200);
  }

  @Test
  void bearerTokenIsIgnored() {
    given().auth().oauth2("a.b.c").get("/user/any-user/token").then().statusCode(200);
  }
}
