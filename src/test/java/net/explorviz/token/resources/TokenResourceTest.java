package net.explorviz.token.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.security.Principal;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.TokenAccessService;
import net.explorviz.token.service.TokenAccessServiceImpl;
import net.explorviz.token.service.TokenPermission;
import net.explorviz.token.service.messaging.TokenEventOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@QuarkusTest
class TokenResourceTest {

  private static final String SECRET = "secret";
  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;
  TokenEventOutbox outbox;
  SnapshotRepository snapshots;
  SecurityIdentity identity;
  private TokenAccessServiceImpl tokenAccessService;

  @BeforeEach
  void setUp() {

    this.repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(this.repo, LandscapeTokenRepository.class);

    this.identity = Mockito.mock(SecurityIdentityProxy.class);
    QuarkusMock.installMockForType(this.identity, SecurityIdentity.class);

    this.outbox = Mockito.mock(TokenEventOutbox.class);
    QuarkusMock.installMockForType(this.outbox, TokenEventOutbox.class);
    this.snapshots = Mockito.mock(SnapshotRepository.class);
    QuarkusMock.installMockForType(this.snapshots, SnapshotRepository.class);

    this.tokenAccessService = Mockito.mock(TokenAccessServiceImpl.class);
    QuarkusMock.installMockForType(this.tokenAccessService, TokenAccessService.class);

    this.inMemRepo = new InMemRepo();
    Mockito.doAnswer(
            invocation -> {
              this.inMemRepo.addToken(invocation.getArgument(0));
              return null;
            })
        .when(this.repo)
        .persist(ArgumentMatchers.any(LandscapeToken.class));
    Mockito.doAnswer(
            invocation -> {
              this.inMemRepo.addToken(invocation.getArgument(0));
              return null;
            })
        .when(this.repo)
        .create(ArgumentMatchers.any(LandscapeToken.class), ArgumentMatchers.any());

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

    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    Mockito.when(
            this.tokenAccessService.canRead(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));
    given()
        .when()
        .get("token/" + value)
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
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("other");

    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));

    // Auth is disabled in tests, all requests get full permissions.
    // Mock to return empty an empty permission array
    Mockito.when(
            this.tokenAccessService.getPermissions(
                ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(new TokenPermission[] {});

    this.repo.persist(new LandscapeToken(value, SECRET, uid, System.currentTimeMillis(), "alias"));

    given().when().get("token/" + value).then().statusCode(403);
  }

  @Test
  void getTokenByUnknownValue() {
    final String value = "unknown";
    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
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

    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.markDeleted(ArgumentMatchers.anyString()))
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

    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.markDeleted(ArgumentMatchers.anyString()))
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
    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    Mockito.when(this.repo.markDeleted(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(value));
    given().when().delete("token/" + value).then().statusCode(404);
  }

  private void givenCallerAndToken(final String caller, final String owner, final String value) {
    final Principal principal = Mockito.mock(Principal.class);
    Mockito.when(this.identity.getPrincipal()).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn(caller);
    Mockito.when(this.repo.findByValue(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));
    this.repo.persist(
        new LandscapeToken(value, SECRET, owner, System.currentTimeMillis(), "alias"));
  }

  @Test
  void getTokenByValueAsSharedUserHidesSecret() {
    givenCallerAndToken("other", "testuid", "token");
    Mockito.when(
            this.tokenAccessService.canRead(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);

    given()
        .when()
        .get("token/token")
        .then()
        .statusCode(200)
        .body("value", is("token"))
        .body("secret", is(""));
  }

  @Test
  void grantAccessWithoutPermission() {
    givenCallerAndToken("other", "testuid", "token");
    given().when().post("token/token/attacker?method=grant").then().statusCode(403);
    Mockito.verify(this.repo, Mockito.never())
        .grantAccess(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
  }

  @Test
  void revokeAccessWithoutPermission() {
    givenCallerAndToken("other", "testuid", "token");
    given().when().post("token/token/testuid?method=revoke").then().statusCode(403);
  }

  @Test
  void cloneTokenWithoutReadPermission() {
    givenCallerAndToken("other", "testuid", "token");
    given().when().post("token/token/other?method=clone").then().statusCode(403);
  }

  @Test
  void modifyAccessWithUnknownMethod() {
    givenCallerAndToken("testuid", "testuid", "token");
    given().when().post("token/token/someone?method=share").then().statusCode(400);
  }

  @Test
  void updateAliasWithoutAlias() {
    givenCallerAndToken("testuid", "testuid", "token");
    Mockito.when(
            this.tokenAccessService.canUpdate(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
        .thenReturn(true);
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .patch("token/token")
        .then()
        .statusCode(400);
  }
}
