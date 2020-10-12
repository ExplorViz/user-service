package net.explorviz.token.generator;

import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.token.model.LandscapeToken;

/**
 * Generates landscape tokens as random UUIDs (UUID v4).
 */
@ApplicationScoped
public class UUIDTokenGenerator implements TokenGenerator {

  @Override
  public LandscapeToken generateToken(String ownerId) {
    String value =  UUID.randomUUID().toString();
    return new LandscapeToken(value, ownerId);
  }
}
