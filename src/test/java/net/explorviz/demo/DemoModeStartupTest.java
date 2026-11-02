package net.explorviz.demo;

import static org.mockito.Mockito.*;

import io.quarkus.runtime.StartupEvent;
import net.explorviz.token.generator.TokenGenerator;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.token.service.TokenServiceImpl;
import net.explorviz.token.service.messaging.EventService;
import net.explorviz.userapi.persistence.UserApiRepository;
import net.explorviz.userapi.service.UserApiServiceImpl;
import org.junit.jupiter.api.Test;

class DemoModeStartupTest {
  @Test
  void demoDoesNotSeedTokensOrEmitEventsAtStartup() throws Exception {
    var tokens = mock(LandscapeTokenRepository.class);
    var apiTokens = mock(UserApiRepository.class);
    var events = mock(EventService.class);
    for (Object service :
        new Object[] {
          new TokenServiceImpl(mock(TokenGenerator.class), tokens, events),
          new UserApiServiceImpl(apiTokens)
        }) {
      DemoMode mode = new DemoMode();
      mode.enabled = true;
      var field = service.getClass().getDeclaredField("demoMode");
      field.setAccessible(true);
      field.set(service, mode);
      var initial = service.getClass().getDeclaredField("initialTokenCreationEnabled");
      initial.setAccessible(true);
      initial.set(service, true);
      var observer = service.getClass().getDeclaredMethod("onStart", StartupEvent.class);
      observer.setAccessible(true);
      observer.invoke(service, new StartupEvent());
    }
    verifyNoInteractions(tokens, apiTokens, events);
  }
}
