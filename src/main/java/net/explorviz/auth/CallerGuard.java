package net.explorviz.auth;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import java.security.Principal;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Checks user ids supplied in query parameters or request bodies against the authenticated caller.
 * All checks pass when OIDC is disabled.
 */
@ApplicationScoped
public class CallerGuard {

  @ConfigProperty(name = "quarkus.oidc.tenant-enabled", defaultValue = "false")
  /* default */ Instance<Boolean> authEnabled;

  @Inject /* default */ SecurityIdentity securityIdentity;

  public boolean isAuthEnabled() {
    return this.authEnabled.get();
  }

  /**
   * Returns the id of the authenticated caller.
   *
   * @return the caller's user id, or {@code null} if the request is anonymous
   */
  public String callerId() {
    final Principal principal = this.securityIdentity.getPrincipal();
    return principal == null ? null : principal.getName();
  }

  /**
   * Checks whether the caller may act as the given user.
   *
   * @param userId the user id supplied by the client
   * @return {@code true} iff OIDC is disabled or the caller is the given user
   */
  public boolean isCaller(final String userId) {
    return !this.isAuthEnabled() || userId != null && userId.equals(this.callerId());
  }

  /**
   * Rejects the request unless the caller may act as the given user.
   *
   * @param userId the user id supplied by the client
   * @throws ForbiddenException if the caller is a different user
   */
  public void requireCaller(final String userId) {
    if (!this.isCaller(userId)) {
      throw new ForbiddenException();
    }
  }
}
