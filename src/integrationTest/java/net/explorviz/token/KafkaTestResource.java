package net.explorviz.token;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Set up Kafka for integration tests. */
public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

  private static final ConfluentKafkaContainer KAFKA =
      new ConfluentKafkaContainer(
          DockerImageName.parse("docker.io/confluentinc/cp-kafka:7.6.1")
              .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

  @Override
  public Map<String, String> start() {
    KAFKA.start();
    final Properties config = new Properties();
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

    final int partitions = 1;
    final short replication = 1;
    final NewTopic topicTokens = new NewTopic("tokens.events", partitions, replication);
    final List<NewTopic> topics = List.of(topicTokens);

    try (AdminClient localKafkaAdmin = AdminClient.create(config)) {
      localKafkaAdmin.createTopics(topics).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while creating Kafka topics", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to create Kafka topics", e);
    }

    return Collections.singletonMap("kafka.bootstrap.servers", KAFKA.getBootstrapServers());
  }

  @Override
  public void stop() {
    KAFKA.stop();
  }
}
