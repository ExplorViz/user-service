package net.explorviz.token.service.messaging;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.util.HashMap;
import java.util.Map;

/**
 * Use the in-memory connector to avoid having to use a broker. See
 * https://github.com/quarkusio/quarkus/issues/6427#issuecomment-623289255
 */
public class FakeKafkaResource implements QuarkusTestResourceLifecycleManager {

  @Override
  public Map<String, String> start() {
    final Map<String, String> props2 =
        InMemoryConnector.switchOutgoingChannelsToInMemory("token-events");
    return new HashMap<>(props2);
  }

  @Override
  public void stop() {
    InMemoryConnector.clear();
  }
}
