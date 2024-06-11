package net.explorviz.token;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Collections;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;

/**
 * Set up MongoDb for integration tests.
 */
public class MongoDbTestResource implements QuarkusTestResourceLifecycleManager {

  private static final GenericContainer<?> MONGO_DB =
      new GenericContainer<>("docker.io/library/mongo:6.0").withExposedPorts(27017);

  @Override
  public Map<String, String> start() {
    MONGO_DB.start();
    return Collections.singletonMap("quarkus.mongodb.connection-string",
        "mongodb://" + MONGO_DB.getHost() + ":" + MONGO_DB.getFirstMappedPort());
  }

  @Override
  public void stop() {
    MONGO_DB.stop();
  }
}
