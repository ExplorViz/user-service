package net.explorviz.token.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import java.util.List;
import net.explorviz.token.model.LandscapeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TokenAccessServiceImplTest {

  private TokenAccessServiceImpl service;
  private final LandscapeToken token =
      new LandscapeToken("v", "s", "owner", 1L, "a", List.of("friend"));

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    this.service = new TokenAccessServiceImpl();
    this.service.authEnabled = Mockito.mock(Instance.class);
    Mockito.when(this.service.authEnabled.get()).thenReturn(true);
  }

  @Test
  void ownerHasAllPermissions() {
    assertTrue(this.service.canRead(this.token, "owner"));
    assertTrue(this.service.canUpdate(this.token, "owner"));
    assertTrue(this.service.canDelete(this.token, "owner"));
  }

  @Test
  void sharedUserCanOnlyRead() {
    assertTrue(this.service.canRead(this.token, "friend"));
    assertFalse(this.service.canUpdate(this.token, "friend"));
    assertFalse(this.service.canDelete(this.token, "friend"));
  }

  @Test
  void strangerHasNoPermissions() {
    assertFalse(this.service.canRead(this.token, "stranger"));
  }
}
