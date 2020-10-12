package net.explorviz.token.service.messaging;

import javax.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
class KafkaConfig {



  private String bootstrapServer;



  private String topicTokenEvents;

  public KafkaConfig(@ConfigProperty(name = "kafka.bootstrap.servers") final String bootstrapServer,
                     @ConfigProperty(name = "explorviz.events.tokens.topic") final String topicTokenEvents) {
    this.bootstrapServer = bootstrapServer;
    this.topicTokenEvents = topicTokenEvents;
  }

  public String getBootstrapServer() {
    return bootstrapServer;
  }


  public String getTopicTokenEvents() {
    return topicTokenEvents;
  }
}
