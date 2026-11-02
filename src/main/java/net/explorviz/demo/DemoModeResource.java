package net.explorviz.demo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/** Exposes the server's mode to clients without a separate frontend setting. */
@Path("/demo-mode")
public class DemoModeResource {
  @Inject DemoMode demoMode;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Boolean> getMode() {
    return Map.of("enabled", demoMode.isEnabled());
  }
}
