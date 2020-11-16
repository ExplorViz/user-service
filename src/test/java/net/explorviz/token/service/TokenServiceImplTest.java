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
import org.mockito.Mockito;

@QuarkusTest
class TokenServiceImplTest {

  @Inject
  TokenService tokenService;

  LandscapeTokenRepository repo;
  InMemRepo inMemRepo;


  @BeforeEach
  void setUp() {
    repo = Mockito.mock(LandscapeTokenRepository.class);
    QuarkusMock.installMockForType(repo, LandscapeTokenRepository.class);
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
  void distinctToken() {
    final String sampleUid = "user|0123";
    LandscapeToken t1 = tokenService.createNewToken(sampleUid);
    LandscapeToken t2 = tokenService.createNewToken(sampleUid);
    assertNotEquals(t1, t2);
  }

  @Test
  void correctOwnerId() {
    final String sampleUid = "user|0123";
    LandscapeToken t1 = tokenService.createNewToken(sampleUid);
    assertEquals(sampleUid, t1.getOwnerId());
  }

  @Test
  void valueNotEmpty() {
    final String sampleUid = "user|0123";
    LandscapeToken t1 = tokenService.createNewToken(sampleUid);
    assertFalse(t1.getValue() == null || t1.getValue().isEmpty() || t1.getValue().isBlank());
  }

  @Test
  void retrieveOnlyOwningToken() {
    final String uid = "testuid";
    LandscapeToken t1 = new LandscapeToken("t1", uid);
    LandscapeToken t2 = new LandscapeToken("t1", "otheruid");
    repo.persist(t1);
    repo.persist(t2);
    Collection<LandscapeToken> got = tokenService.getOwningTokens(uid);
    assertEquals(1, got.size());
    assertTrue(got.contains(t1));
  }

  @Test
  void getByTokenValue() {
    final String value = "t1";
    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue(value));

    final String uid = "testuid";

    LandscapeToken t1 = new LandscapeToken("t1", uid);
    repo.persist(t1);
    Optional<LandscapeToken> got = tokenService.getByValue(value);
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

    Mockito.when(repo.find(Mockito.anyString(), Mockito.<String>anyVararg()))
        .thenAnswer(invocation -> inMemRepo.findByValue("other"));

    LandscapeToken t1 = new LandscapeToken(value, uid);
    repo.persist(t1);
    Optional<LandscapeToken> got = tokenService.getByValue("other");
    assertFalse(got.isPresent());
  }

  @Test
  void retrieveMultipleToken() {
    final String uid = "testuid";
    for (int i = 0; i < 100; i++) {
      repo.persist(new LandscapeToken(String.valueOf(i), uid));
    }
    Collection<LandscapeToken> got = tokenService.getOwningTokens(uid);
    assertTrue(got.containsAll(repo.findForUser(uid)));
  }

  @Test
  void deleteExisting() {
    final String uid = "uid";
    final String tokenValue = "token";
    Mockito.when(repo.delete(Mockito.anyString(), Mockito.<String>anyVararg())).thenAnswer(
        invocation -> inMemRepo.deleteByValue(tokenValue));
    LandscapeToken t = new LandscapeToken(tokenValue, uid);
    repo.persist(t);
    tokenService.deleteByValue(t);
    assertEquals(0, inMemRepo.size());
  }


}





