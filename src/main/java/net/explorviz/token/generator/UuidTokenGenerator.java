package net.explorviz.token.generator;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.UUID;
import net.explorviz.token.model.LandscapeToken;
import org.apache.commons.lang3.RandomStringUtils;

/** Generates landscape tokens as random UUIDs (UUID v4). */
@ApplicationScoped
public class UuidTokenGenerator implements TokenGenerator {

  private static final int SECRET_LEN = 16;

  @Override
  public LandscapeToken generateToken(final String ownerId, final String alias) {
    return this.generateToken(ownerId, alias, null, null);
  }

  @Override
  public LandscapeToken generateToken(
      final String ownerId,
      final String alias,
      final String valueOverride,
      final String secretOverride) {

    final String value =
        valueOverride == null || valueOverride.isBlank()
            ? UUID.randomUUID().toString()
            : valueOverride.trim();
    final long created = System.currentTimeMillis();

    final String secret =
        secretOverride == null || secretOverride.isBlank()
            ? RandomStringUtils.random(SECRET_LEN, 0, 0, true, true, null, new SecureRandom())
            : secretOverride.trim();

    return new LandscapeToken(value, secret, ownerId, created, alias, Collections.emptyList());
  }
}
