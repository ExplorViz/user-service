package net.explorviz.token.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LandscapeTokenTest {

  @Test
  void toProtobufToleratesNullAttributes() {
    final LandscapeToken token = new LandscapeToken("v", null, "owner", 1L, null);
    final var proto = token.toProtobuf();
    assertEquals("", proto.getAlias());
    assertEquals("", proto.getSecret());
  }
}
