package net.explorviz.token;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.service.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
@QuarkusTestResource(KafkaTestResource.class)
class TokenServiceImplIntegrationTest {

  @Inject
  TokenService tokenService;

  private static final String USER_1 = "user1";
  private static final String USER_2 = "user2";

  private LandscapeToken token;

  /*
   * This test requires a running mongodb on port 27017. You can start one with:
   *
   * docker run -ti --rm -p 27017:27017 mongo:4.0
   */
  @Test
  void grantAndRevokeAccessToToken() {
    givenTokenForUser1();

    thenTokensSharedWithUser2(0);

    whenGrantingAccessToUser2();

    thenTokensSharedWithUser2(1);

    whenRevokingAccessForUser2();

    thenTokensSharedWithUser2(0);
  }

  @AfterEach
  void tearDown() {
    this.tokenService.deleteByValue(token);
  }

  private void givenTokenForUser1() {
    tokenService.createNewToken(USER_1);
    var tokensOwnedByUser1 = this.tokenService.getOwningTokens(USER_1);
    assertEquals(1, tokensOwnedByUser1.size());
    token = tokensOwnedByUser1.iterator().next();
  }

  private void whenGrantingAccessToUser2() {
    this.tokenService.grantAccess(token, USER_2);
  }

  private void whenRevokingAccessForUser2() {
    this.tokenService.revokeAccess(token, USER_2);
  }

  private void thenTokensSharedWithUser2(int number) {
    var tokensSharedWithUser2 = this.tokenService.getSharedTokens(USER_2);
    assertEquals(number, tokensSharedWithUser2.size());
  }
}


