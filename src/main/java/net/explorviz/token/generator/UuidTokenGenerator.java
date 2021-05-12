package net.explorviz.token.generator;

import java.util.Collections;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Generates landscape tokens as random UUIDs (UUID v4).
 */
@ApplicationScoped
public class UuidTokenGenerator implements TokenGenerator {

  private static final int SECRET_LEN = 16;

  @Override
  public LandscapeToken generateToken(final String ownerId, final String alias) {

    final String value = UUID.randomUUID().toString();
    final long created = System.currentTimeMillis();

    // 16-char secret
    // Not cryptographically secure!
    final String secret = RandomStringUtils.randomAlphanumeric(SECRET_LEN);



    return new LandscapeToken(value, secret, ownerId, created, alias, Collections.emptyList());
  }
}
