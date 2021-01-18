package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.security.Principal;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenAccessServiceImpl;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class TokenResourceTest {

  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;
  SecurityIdentity identity;


  @BeforeEach
  void setUp() {


    repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(repo, LandscapeTokenRepository.class);

    identity = Mockito.mock(SecurityIdentityProxy.class);
    QuarkusMock.installMockForType(identity, SecurityIdentity.class);


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
  void getTokenByValue() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    Principal principal = Mockito.mock(Principal.class);
    Mockito.when(identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn(uid);

    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));


    repo.persist(new LandscapeToken(value, uid));
    given()
        .when().get("token/" + value)
        .then()
        .statusCode(200)
        .body("ownerId", is(uid))
        .body("value", is(value));
  }

  @Test
  void getTokenByValueWithoutPermission() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    Principal principal = Mockito.mock(Principal.class);
    Mockito.when(identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("other");

    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));



    repo.persist(new LandscapeToken(value, uid));
    given()
        .when().get("token/" + value)
        .then()
        .statusCode(403);
  }

  @Test
  void getTokenByUnknownValue() {
    String value = "unknown";
    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));

    given()
        .when().get("token/"+ value)
        .then()
        .statusCode(404);
  }

  @Test
  void deleteToken() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    Principal principal = Mockito.mock(Principal.class);
    Mockito.when(identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn(uid);


    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));
    Mockito.when(repo.delete(Mockito.anyString(), Mockito.<String>anyVararg())).thenAnswer(
        invocation -> inMemRepo.deleteByValue(value));

    repo.persist(new LandscapeToken(value, uid));

    given()
        .when().delete("token/" + value)
        .then()
        .statusCode(204);
    given()
        .when().get("token/" + value)
        .then()
        .statusCode(404);
  }

  @Test
  void deleteTokenWithoutPermission() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    Principal principal = Mockito.mock(Principal.class);
    Mockito.when(identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("other");


    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));
    Mockito.when(repo.delete(Mockito.anyString(), Mockito.<String>anyVararg())).thenAnswer(
        invocation -> inMemRepo.deleteByValue(value));

    repo.persist(new LandscapeToken(value, uid));

    given()
        .when().delete("token/" + value)
        .then()
        .statusCode(403);
    Mockito.when(principal.getName()).thenReturn(uid);
    given()
        .when().get("token/" + value)
        .then()
        .statusCode(200);
  }

  @Test
  void deleteUnknownToken() {

    String value = "unknown";
    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));
    Mockito.when(repo.delete(Mockito.anyString(), Mockito.<String>anyVararg())).thenAnswer(
        invocation -> inMemRepo.deleteByValue(value));
    given()
        .when().delete("token/" + value)
        .then()
        .statusCode(404);
  }
}
