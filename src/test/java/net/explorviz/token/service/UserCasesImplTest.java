package net.explorviz.token.service;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import net.explorviz.token.model.LandscapeToken;
import org.junit.jupiter.api.Test;


class UserCasesImplTest {

  @QuarkusTest
  static class TokenCreation {

    @Inject
    UseCases useCases;

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
