package net.explorviz.token.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Collection;
import java.util.Optional;
import javax.inject.Inject;
import net.explorviz.token.InMemRepo;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@QuarkusTest
class TokenServiceImplTest {

  @Inject
  TokenService tokenService;

  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;


  @BeforeEach
  void setUp() {
    this.repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(this.repo, LandscapeTokenRepository.class);
    final EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
    QuarkusMock.installMockForType(mockEventService, EventService.class);

    this.inMemRepo = new InMemRepo();
    Mockito.doAnswer(invocation -> {
      this.inMemRepo.addToken(invocation.getArgument(0));
      return null;
    }).when(this.repo).persist(ArgumentMatchers.any(LandscapeToken.class));

    Mockito.when(this.repo.findForUser(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findForUser(invocation.getArgument(0)));

    Mockito.when(this.repo.findSharedForUser(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findSharedForUser(invocation.getArgument(0)));
  }


  @Test
  void distinctToken() {
    final String sampleUid = "user|0123";
    final LandscapeToken t1 = this.tokenService.createNewToken(sampleUid);
    final LandscapeToken t2 = this.tokenService.createNewToken(sampleUid);
    assertNotEquals(t1, t2);
  }

  @Test
  void correctOwnerId() {
    final String sampleUid = "user|0123";
    final LandscapeToken t1 = this.tokenService.createNewToken(sampleUid);
    assertEquals(sampleUid, t1.getOwnerId());
  }

  @Test
  void valueNotEmpty() {
    final String sampleUid = "user|0123";
    final LandscapeToken t1 = this.tokenService.createNewToken(sampleUid);
    assertFalse(t1.getValue() == null || t1.getValue().isEmpty() || t1.getValue().isBlank());
  }

  @Test
  void secretNotEmpty() {
    final String sampleUid = "user|0123";
    final LandscapeToken t1 = this.tokenService.createNewToken(sampleUid);
    System.out.println("secret: " + t1.getSecret());
    assertFalse(t1.getSecret() == null || t1.getSecret().isEmpty() || t1.getSecret().isBlank());
  }

  @Test
  void retrieveOnlyOwningToken() {
    final String uid = "testuid";
    final LandscapeToken t1 =
        new LandscapeToken("t1", "secret", uid, System.currentTimeMillis(), "t1");
    final LandscapeToken t2 =
        new LandscapeToken("t1", "secret", "otheruid", System.currentTimeMillis(), "t2");
    this.repo.persist(t1);
    this.repo.persist(t2);
    final Collection<LandscapeToken> got = this.tokenService.getOwningTokens(uid);
    assertEquals(1, got.size());
    assertTrue(got.contains(t1));
  }

  @Test
  void retrieveSharedTokens() {
    final String uid = "testuid";
    final LandscapeToken t1 =
        new LandscapeToken("t1", "secret", uid, System.currentTimeMillis(), "t1");
    final LandscapeToken t2 =
        new LandscapeToken("t1", "secret", "otheruid", System.currentTimeMillis(), "t2");
    t2.getSharedUsersIds().add(uid);
    this.repo.persist(t1);
    this.repo.persist(t2);
    final Collection<LandscapeToken> got = this.tokenService.getSharedTokens(uid);
    assertEquals(1, got.size());
    assertTrue(got.contains(t2));
  }

  @Test
  void getByTokenValue() {
    final String value = "t1";
    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue(value));

    final String uid = "testuid";

    final LandscapeToken t1 =
        new LandscapeToken("t1", "secret", uid, System.currentTimeMillis(), "");
    this.repo.persist(t1);
    final Optional<LandscapeToken> got = this.tokenService.getByValue(value);
    if (got.isPresent()) {
      assertEquals(t1, got.get());
    } else {
      fail();
    }
  }

  @Test
  void getByUnknownTokenValue() {
    final String uid = "testuid";
    final String value = "t1";

    Mockito.when(this.repo.find(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.findByValue("other"));

    final LandscapeToken t1 =
        new LandscapeToken(value, "secret", uid, System.currentTimeMillis(), "");
    this.repo.persist(t1);
    final Optional<LandscapeToken> got = this.tokenService.getByValue("other");
    assertFalse(got.isPresent());
  }

  @Test
  void retrieveMultipleToken() {
    final String uid = "testuid";
    for (int i = 0; i < 100; i++) {
      this.repo.persist(
          new LandscapeToken(String.valueOf(i), "secret", uid, System.currentTimeMillis(),
              "alias"));
    }
    final Collection<LandscapeToken> got = this.tokenService.getOwningTokens(uid);
    assertTrue(got.containsAll(this.repo.findForUser(uid)));
  }

  @Test
  void deleteExisting() {
    final String uid = "uid";
    final String tokenValue = "token";
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(), ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(tokenValue));
    final LandscapeToken t =
        new LandscapeToken(tokenValue, "secret", uid, System.currentTimeMillis(), "");
    this.repo.persist(t);
    this.tokenService.deleteByValue(t);
    assertEquals(0, this.inMemRepo.size());
  }


}


