package net.explorviz.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.jwt.build.Jwt;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Authentication can be switched on with the variable that the deployment sets at runtime. */
@QuarkusTest
@TestProfile(AuthenticationEnabledTest.AuthEnabledProfile.class)
class AuthenticationEnabledTest {

  private static final String ISSUER = "http://localhost/realms/test";
  private static final String SIGNING_KEY = "test.jwt.signing-key";
  private static final String USER = "user-1";
  private static final String OWN_TOKENS = "/user/" + USER + "/token";

  @ConfigProperty(name = SIGNING_KEY)
  String signingKey;

  @BeforeEach
  void setUp() {
    final LandscapeTokenRepository repository = Mockito.mock(LandscapeTokenRepository.class);
    Mockito.when(repository.findForUser(USER))
        .thenReturn(
            new ArrayList<>(List.of(new LandscapeToken("value", "secret", USER, 1L, "alias"))));
    QuarkusMock.installMockForType(repository, LandscapeTokenRepository.class);
  }

  @Test
  void anonymousCallerIsRejected() {
    given().get(OWN_TOKENS).then().statusCode(401);
  }

  @Test
  void invalidTokenIsRejected() {
    given().auth().oauth2("a.b.c").get(OWN_TOKENS).then().statusCode(401);
  }

  @Test
  void tokenWithForeignSignatureIsRejected() {
    given()
        .auth()
        .oauth2(jwt(newKeyPair().getPrivate(), USER))
        .get(OWN_TOKENS)
        .then()
        .statusCode(401);
  }

  @Test
  void callerAccessesOwnTokens() {
    given()
        .auth()
        .oauth2(this.jwt(USER))
        .get(OWN_TOKENS)
        .then()
        .statusCode(200)
        .body("size()", is(1));
  }

  @Test
  void callerCannotAccessTokensOfOtherUser() {
    given().auth().oauth2(this.jwt("user-2")).get(OWN_TOKENS).then().statusCode(403);
  }

  @Test
  void listOfAllTokensIsNotAvailable() {
    given().auth().oauth2(this.jwt(USER)).get("/tokens").then().statusCode(404);
  }

  private String jwt(final String subject) {
    try {
      final byte[] key = Base64.getDecoder().decode(this.signingKey);
      return jwt(
          KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key)), subject);
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String jwt(final PrivateKey key, final String subject) {
    return Jwt.issuer(ISSUER).subject(subject).expiresIn(300).sign(key);
  }

  private static KeyPair newKeyPair() {
    try {
      final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Enables authentication and verifies tokens with a local key instead of a Keycloak server.
   * Quarkus loads profiles in another class loader than tests, so keys are shared via config.
   */
  public static class AuthEnabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      final KeyPair keys = newKeyPair();
      final Base64.Encoder encoder = Base64.getEncoder();
      return Map.of(
          "QUARKUS_OIDC_ENABLED",
          "true",
          "quarkus.oidc.auth-server-url",
          "",
          "quarkus.oidc.jwks-path",
          "",
          "quarkus.oidc.token.issuer",
          ISSUER,
          "quarkus.oidc.public-key",
          encoder.encodeToString(keys.getPublic().getEncoded()),
          SIGNING_KEY,
          encoder.encodeToString(keys.getPrivate().getEncoded()));
    }
  }
}
