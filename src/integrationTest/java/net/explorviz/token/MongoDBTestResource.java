package net.explorviz.token;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Collections;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;

public class MongoDBTestResource implements QuarkusTestResourceLifecycleManager {

    private static final GenericContainer MONGO_DB = new GenericContainer<>("mongo:4.2").withExposedPorts(27017);

    @Override
    public Map<String, String> start() {
      MONGO_DB.start();
      return Collections.singletonMap("quarkus.mongodb.connection-string", 
                 "mongodb://" + MONGO_DB.getContainerIpAddress() + ":" + MONGO_DB.getFirstMappedPort());
    }

    @Override
    public void stop() {
        MONGO_DB.stop();
    }
}
