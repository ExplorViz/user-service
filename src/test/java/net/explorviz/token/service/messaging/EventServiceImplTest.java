package net.explorviz.token.service.messaging;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySink;
import java.util.Collections;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import net.explorviz.avro.EventType;
import net.explorviz.avro.TokenEvent;
import net.explorviz.token.model.LandscapeToken;
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
    final String tokenValue = "testtoken";
    final String uid = "testuid";
    final LandscapeToken token =
        new LandscapeToken(tokenValue, "secret", uid, 0, "", Collections.emptyList());

    final TokenEvent testEvent = TokenEvent.newBuilder().setToken(token.toAvro())
        .setType(EventType.CREATED).setClonedToken("").build();

    final InMemorySink<TokenEvent> events = this.connector.sink("token-events");
    this.service.dispatch(testEvent);

    final TokenEvent got = events.received().get(0).getPayload();
    Assertions.assertEquals(testEvent, got);
  }

}
