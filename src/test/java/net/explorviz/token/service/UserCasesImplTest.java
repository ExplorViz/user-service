package net.explorviz.token.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class UserCasesImplTest {

  @QuarkusTest
  static class TokenCreation {

    @Inject
    UseCases useCases;

    LandscapeTokenRepository mockRepo;

    @BeforeEach
    void setUp() {
      mockRepo = Mockito.mock(LandscapeTokenRepository.class);
      QuarkusMock.installMockForType(mockRepo, LandscapeTokenRepository.class);
    }

    @Test
    void distinctToken() {
      final String sampleUid = "user|0123";
      LandscapeToken t1 = useCases.createNewToken(sampleUid);
      LandscapeToken t2 = useCases.createNewToken(sampleUid);
      assertNotEquals(t1, t2);
    }

    @Test
    void correctOwnerId() {
      final String sampleUid = "user|0123";
      LandscapeToken t1 = useCases.createNewToken(sampleUid);
      assertEquals(sampleUid, t1.getOwnerId());
    }

    @Test
    void valueNotEmpty() {
      final String sampleUid = "user|0123";
      LandscapeToken t1 = useCases.createNewToken(sampleUid);
      assertFalse(t1.getValue() == null || t1.getValue().isEmpty() || t1.getValue().isBlank());
    }

  }

}
