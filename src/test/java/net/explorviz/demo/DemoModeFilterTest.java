package net.explorviz.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DemoModeFilterTest {
  @Test
  void demoBlocksAllMutatingHttpMethods() {
    DemoMode mode = new DemoMode();
    mode.enabled = true;
    DemoModeFilter filter = new DemoModeFilter();
    filter.demoMode = mode;
    for (String method : new String[] {"POST", "PUT", "PATCH", "DELETE"}) {
      ContainerRequestContext request = mock(ContainerRequestContext.class);
      when(request.getMethod()).thenReturn(method);
      filter.filter(request);
      ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
      verify(request).abortWith(response.capture());
      assertEquals(403, response.getValue().getStatus());
      assertTrue(response.getValue().getEntity().toString().contains("DEMO_MODE"));
    }
  }

  @Test
  void readsAndNormalModeRemainAvailable() {
    DemoMode mode = new DemoMode();
    DemoModeFilter filter = new DemoModeFilter();
    filter.demoMode = mode;
    for (boolean enabled : new boolean[] {false, true}) {
      mode.enabled = enabled;
      for (String method :
          enabled
              ? new String[] {"GET", "HEAD", "OPTIONS"}
              : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE"}) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn(method);
        filter.filter(request);
        verify(request, never()).abortWith(any());
      }
    }
  }
}
