package net.explorviz.token.service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@ApplicationScoped
class EventServiceImpl implements EventService {

  private KafkaProducer<String, String> producer;
  private String eventTopic;

  @Inject
  public EventServiceImpl(KafkaConfig kafkaConfig) {
    final Properties kafkaProps = new Properties();
    kafkaProps.put("bootstrap.servers", kafkaConfig.getBootstrapServer());
    kafkaProps
        .put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"); // NOCS
    kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    this.producer = new KafkaProducer<>(kafkaProps);

    this.eventTopic = kafkaConfig.getTopicTokenEvents();
  }

  @Override
  public void dispatch(final TokenEvent event) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      final String eventJson = mapper.writeValueAsString(event);
      final ProducerRecord<String, String> record =
          new ProducerRecord<>(eventTopic, event.getToken().getOwnerId(), eventJson);
      this.producer.send(record);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

  }

}
