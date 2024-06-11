package net.explorviz.token;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Set up Kafka for integration tests.
 */
public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

  private static final KafkaContainer KAFKA = new KafkaContainer(
      DockerImageName.parse("docker.io/confluentinc/cp-kafka:7.3.0")
          .asCompatibleSubstituteFor("confluentinc/cp-kafka:7.3.0")).withKraft();
      //DockerImageName.parse("confluentinc/cp-kafka:7.3.0");

  @Override
  public Map<String, String> start() {
    KAFKA.start();
    final Properties config = new Properties();
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

    final AdminClient localKafkaAdmin = AdminClient.create(config);

    final int partitions = 1;
    final short replication = 1;
    final NewTopic topicTokens = new NewTopic("token-events", partitions, replication);
    final List<NewTopic> topics = List.of(topicTokens);

    localKafkaAdmin.createTopics(topics);

    return Collections.singletonMap("kafka.bootstrap.servers", KAFKA.getBootstrapServers());
  }

  @Override
  public void stop() {
    KAFKA.stop();
  }
}
