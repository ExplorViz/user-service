package net.explorviz.token.service.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import net.explorviz.proto.EventType;
import net.explorviz.proto.LandscapeToken;
import net.explorviz.proto.TokenEvent;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class EventServiceImplFailureTest {

  private final EventServiceImpl service = new EventServiceImpl();
  private Emitter<byte[]> emitter;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    this.emitter = Mockito.mock(Emitter.class);
    this.service.eventEmitter = this.emitter;
  }

  private CompletableFuture<Void> dispatch(final EventType type) {
    return this.service
        .dispatch(
            TokenEvent.newBuilder()
                .setType(type)
                .setToken(LandscapeToken.newBuilder().setId("t"))
                .build())
        .toCompletableFuture();
  }

  @SuppressWarnings("unchecked")
  private Message<byte[]> sent() {
    final ArgumentCaptor<Message<byte[]>> message = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.emitter).send(message.capture());
    return message.getValue();
  }

  @Test
  void succeedsWithAcknowledgement() {
    final CompletableFuture<Void> result = this.dispatch(EventType.EVENT_TYPE_DELETED);
    assertFalse(result.isDone());
    this.sent().ack();
    assertTrue(result.isDone() && !result.isCompletedExceptionally());
  }

  @Test
  void failsIfBrokerRejectsRecord() {
    final CompletableFuture<Void> result = this.dispatch(EventType.EVENT_TYPE_CREATED);
    this.sent().nack(new IllegalStateException("Kafka is not available"));
    assertTrue(result.isCompletedExceptionally());
  }

  @Test
  void failsIfChannelDoesNotAcceptMessages() {
    Mockito.doThrow(new IllegalStateException("full"))
        .when(this.emitter)
        .send(ArgumentMatchers.<Message<byte[]>>any());
    assertTrue(this.dispatch(EventType.EVENT_TYPE_CREATED).isCompletedExceptionally());
  }
}
