package net.explorviz.token.service.messaging;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySink;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.TokenEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(FakeKafkaResource.class)
class EventServiceImplTest {


  @Inject
  EventServiceImpl service;


  @Inject
  @Any
  InMemoryConnector connector;

  @Test
  void dispatchEvent() {
    final String token = "testtoken";
    final String uid = "testuid";
    TokenEvent testEvent = TokenEvent.newBuilder()
        .setToken(token)
        .setUserId(uid)
        .setType(EventType.CREATED)
        .build();

    InMemorySink<TokenEvent> events = connector.sink("token-events");
    service.dispatch(testEvent);

    TokenEvent got = events.received().get(0).getPayload();
    Assertions.assertEquals(testEvent, got);
  }

}
