package net.explorviz.token.service.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.smallrye.reactive.messaging.kafka.tracing.KafkaOpenTelemetryInstrumenter;
import io.smallrye.reactive.messaging.kafka.tracing.KafkaTrace;
import jakarta.enterprise.inject.Instance;
import net.explorviz.proto.EventType;
import net.explorviz.proto.LandscapeToken;
import net.explorviz.proto.TokenEvent;
import net.explorviz.telemetry.KafkaRecordSpanProcessor;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class KafkaRecordTelemetryTest {

  @Mock Emitter<byte[]> emitter;
  @Mock Instance<OpenTelemetry> telemetryInstance;
  @Captor ArgumentCaptor<Message<byte[]>> records;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void capturesFullProtobufContentOnConnectorProducerSpan() throws Exception {
    assertRecordTelemetry(EventType.EVENT_TYPE_CREATED);
  }

  @Test
  void identifiesTombstonesWithoutInventingAPayload() throws Exception {
    assertRecordTelemetry(EventType.EVENT_TYPE_DELETED);
  }

  @SuppressWarnings("try")
  private void assertRecordTelemetry(EventType type) throws Exception {
    var recorder = mock(SpanProcessor.class);
    when(recorder.isEndRequired()).thenReturn(true);
    when(recorder.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    try (var provider =
        SdkTracerProvider.builder()
            .addSpanProcessor(new KafkaRecordSpanProcessor())
            .addSpanProcessor(recorder)
            .build()) {
      var telemetry = OpenTelemetrySdk.builder().setTracerProvider(provider).build();
      when(telemetryInstance.isResolvable()).thenReturn(true);
      when(telemetryInstance.get()).thenReturn(telemetry);
      var event =
          TokenEvent.newBuilder()
              .setType(type)
              .setToken(LandscapeToken.newBuilder().setId("token1").setSecret("secret1"))
              .build();
      var service = new EventServiceImpl();
      service.eventEmitter = emitter;
      var parent = provider.get("test-http-server").spanBuilder("POST /tokens").startSpan();
      try (var scope = parent.makeCurrent()) {
        service.dispatch(event);
      }
      verify(emitter).send(records.capture());
      var message = records.getValue();

      // Run the connector after leaving the dispatch context, as for an asynchronous send.
      var trace =
          new KafkaTrace.Builder()
              .withTopic("tokens.events")
              .withClientId("test-producer")
              .withHeaders(new RecordHeaders())
              .build();
      KafkaOpenTelemetryInstrumenter.createForSink(telemetryInstance).traceOutgoing(message, trace);

      var captured = ArgumentCaptor.forClass(ReadableSpan.class);
      verify(recorder).onEnd(captured.capture());
      var data = captured.getValue().toSpanData();
      var attributes = data.getAttributes();
      assertEquals(SpanKind.PRODUCER, data.getKind());
      assertEquals(parent.getSpanContext().getSpanId(), data.getParentSpanId());
      assertEquals("token1", attributes.get(AttributeKey.stringKey("messaging.kafka.message.key")));
      boolean tombstone = type == EventType.EVENT_TYPE_DELETED;
      assertEquals(
          tombstone, attributes.get(AttributeKey.booleanKey("explorviz.messaging.tombstone")));
      if (tombstone) {
        assertNull(message.getPayload());
        assertNull(attributes.get(AttributeKey.stringKey("messaging.message.body")));
        assertEquals(0L, attributes.get(AttributeKey.longKey("messaging.message.body.size")));
      } else {
        assertEquals(event, TokenEvent.parseFrom(message.getPayload()));
        assertEquals(
            event.toString(), attributes.get(AttributeKey.stringKey("messaging.message.body")));
        assertEquals(
            (long) event.getSerializedSize(),
            attributes.get(AttributeKey.longKey("messaging.message.body.size")));
      }
      assertNull(
          ((ReadableSpan) parent).getAttribute(AttributeKey.stringKey("messaging.message.body")));
      parent.end();
    }
  }
}
