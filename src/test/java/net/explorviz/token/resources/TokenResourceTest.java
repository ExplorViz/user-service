package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.security.Principal;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenAccessServiceImpl;
import net.explorviz.token.service.TokenPermission;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@QuarkusTest
class TokenResourceTest {

  private static final String SECRET = "secret";
  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;
  SecurityIdentity identity;
  private TokenAccessServiceImpl tokenAccessService;

  @BeforeEach
  void setUp() {

    this.repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(this.repo, LandscapeTokenRepository.class);

    this.identity = Mockito.mock(SecurityIdentityProxy.class);
    QuarkusMock.installMockForType(this.identity, SecurityIdentity.class);

    final EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
    QuarkusMock.installMockForType(mockEventService, EventService.class);

    this.tokenAccessService = Mockito.mock(TokenAccessServiceImpl.class);
    QuarkusMock.installMockForType(this.tokenAccessService, TokenAccessService.class);

    this.inMemRepo = new InMemRepo();
    Mockito.doAnswer(invocation -> {
      this.inMemRepo.addToken(invocation.getArgument(0));
      return null;
    }).when(this.repo).persist(ArgumentMatchers.any(LandscapeToken.class));

    Mockito.when(this.repo.findForUser(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findForUser(invocation.getArgument(0)));
  }


  @Test
  void getTokenByValue() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn(uid);

    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    Mockito.when(
            this.tokenAccessService.canRead(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));
    given().when().get("token/" + value).then().statusCode(200).body("ownerId", is(uid))
        .body("value", is(value));
  }

  @Test
  void getTokenByValueWithoutPermission() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("other");

    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));

    // Auth is disabled in tests, all requests get full permissions.
    // Mock to return empty an empty permission array
    Mockito.when(this.tokenAccessService.getPermissions(ArgumentMatchers.any(),
        ArgumentMatchers.anyString())).thenReturn(new TokenPermission[] {});

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));

    given().when().get("token/" + value).then().statusCode(403);
  }

  @Test
  void getTokenByUnknownValue() {
    final String value = "unknown";
    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    given().when().get("token/" + value).then().statusCode(404);
  }

  @Test
  void deleteToken() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn(uid);

    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(value));

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), ""));

    Mockito.when(
            this.tokenAccessService.canDelete(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);

    given().when().delete("token/" + value).then().statusCode(204);
    given().when().get("token/" + value).then().statusCode(404);
  }

  @Test
  void deleteTokenWithoutPermission() {

    final String uid = "testuid";
    final String value = "token";

    // Mock username
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("other");

    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(value));

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));

    // Can read but not delete
    Mockito.when(
            this.tokenAccessService.canRead(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);

    given().when().delete("token/" + value).then().statusCode(403);
    Mockito.when(principal.getName()).thenReturn(uid);
    given().when().get("token/" + value).then().statusCode(200);
  }

  @Test
  void deleteUnknownToken() {

    final String value = "unknown";
    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(value));
    given().when().delete("token/" + value).then().statusCode(404);
  }
}
