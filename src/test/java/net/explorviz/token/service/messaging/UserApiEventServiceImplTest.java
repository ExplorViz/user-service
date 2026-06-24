package net.explorviz.token.service.messaging;

import com.google.protobuf.InvalidProtocolBufferException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.util.Collections;
import net.explorviz.proto.EventType;
import net.explorviz.proto.TokenEvent;
import net.explorviz.token.model.LandscapeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(FakeKafkaResource.class)
class UserApiEventServiceImplTest {

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

    final TokenEvent testEvent =
        TokenEvent.newBuilder().setToken(token.toProtobuf()).setType(EventType.EVENT_TYPE_CREATED).build();

    final InMemorySink<byte[]> events = this.connector.sink("token-events");
    this.service.dispatch(testEvent);

    final TokenEvent got;
    try {
      got = TokenEvent.parseFrom(events.received().getFirst().getPayload());
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    Assertions.assertEquals(testEvent, got);
  }

}
