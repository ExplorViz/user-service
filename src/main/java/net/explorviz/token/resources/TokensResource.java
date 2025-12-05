package net.explorviz.token.resources;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;
import net.explorviz.token.model.LandscapeToken;
import net.explorviz.token.service.TokenService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * HTTP endpoint to get all {@link LandscapeToken}s.
 * This endpoint is only available when OIDC is disabled or in dev mode.
 */
@Path("tokens")
@RequestScoped
public class TokensResource {

  private final TokenService tokenService;

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  /* default */ Instance<Boolean> authEnabled; // NOCS

  @Inject
  LaunchMode launchMode;

  @Inject
  public TokensResource(final TokenService tokenService) {
    this.tokenService = tokenService;
  }

  /**
   * Endpoint to get all landscape tokens.
   * This endpoint is only available when OIDC is disabled or in dev mode.
   *
   * @return Collection of all landscape tokens.
   * @throws NotFoundException if OIDC is enabled and not in dev mode.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  public Collection<LandscapeToken> getAllTokens() {
    final boolean isDevMode = launchMode == LaunchMode.DEVELOPMENT;
    final boolean isOidcDisabled = !this.authEnabled.get();
    
    if (!isOidcDisabled && !isDevMode) {
      throw new NotFoundException(
          "This endpoint is only available when OIDC is disabled or in dev mode");
    }
    return this.tokenService.getAllTokens();
  }
}

