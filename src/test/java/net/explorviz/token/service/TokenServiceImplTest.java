package net.explorviz.token.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Collection;
import javax.inject.Inject;
import net.explorviz.token.TestUtils;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.token.service.messaging.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class TokenServiceImplTest {

  @QuarkusTest
  static class TokenCreation {

    @Inject
    TokenService tokenService;

    LandscapeTokenRepository mockRepo;

    @BeforeEach
    void setUp() {
      mockRepo = Mockito.mock(LandscapeTokenRepository.class);
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
      EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
      QuarkusMock.installMockForType(mockEventService, EventService.class);
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

  }

  @QuarkusTest
  static class TokenRetrieval {

    @Inject
    TokenService tokenService;

    TestUtils.MockRepo mockRepo;

    @BeforeEach
    void setUp() {
      mockRepo = new TestUtils.MockRepo();
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
      EventServiceImpl mockEventService = Mockito.mock(EventServiceImpl.class);
      QuarkusMock.installMockForType(mockEventService, EventService.class);
    }

    @Test
    void retrieveOnlyOwningToken() {
      final String uid = "testuid";
      LandscapeToken t1 = new LandscapeToken("t1", uid);
      LandscapeToken t2 = new LandscapeToken("t1", "otheruid");
      mockRepo.persist(t1);
      mockRepo.persist(t2);
      Collection<LandscapeToken> got = tokenService.getOwningTokens(uid);
      assertEquals(1, got.size());
      assertTrue(got.contains(t1));
    }

    @Test
    void retrieveMultipleToken() {
      final String uid = "testuid";
      for (int i= 0; i<100; i++) {
        mockRepo.persist(new LandscapeToken(String.valueOf(i), uid));
      }
      Collection<LandscapeToken> got = tokenService.getOwningTokens(uid);
      assertTrue(got.containsAll(mockRepo.getTokens()));
    }


  }

}
