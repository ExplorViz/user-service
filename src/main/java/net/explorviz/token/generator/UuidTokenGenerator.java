package net.explorviz.token.generator;

import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;

/**
 * Generates landscape tokens as random UUIDs (UUID v4).
 */
@ApplicationScoped
public class UuidTokenGenerator implements TokenGenerator {

  @Override
  public LandscapeToken generateToken(final String ownerId, final String alias) {
    final String value = UUID.randomUUID().toString();
    final long created = System.currentTimeMillis();
    return new LandscapeToken(value, ownerId, created, alias);
  }
}
