package net.explorviz.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterId;
import com.mongodb.connection.ConnectionDescription;
import com.mongodb.connection.ServerId;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.mongodb.runtime.MongoTracingRuntimeConfig;
import io.quarkus.mongodb.runtime.MongoTracingRuntimeConfig.CommandDetailLevel;
import io.quarkus.mongodb.tracing.MongoTracingCommandListener;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MongoDbSpanProcessorTest {

  @Test
  void mongoCommandExportsSystemIdentityAndFullQuery() {
    var recorder = mock(SpanProcessor.class);
    when(recorder.isEndRequired()).thenReturn(true);
    when(recorder.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    try (var provider = SdkTracerProvider.builder()
        .addSpanProcessor(new MongoDbSpanProcessor())
        .addSpanProcessor(recorder)
        .build()) {
      var telemetry = OpenTelemetrySdk.builder().setTracerProvider(provider).build();
      var tracing = mock(MongoTracingRuntimeConfig.class);
      when(tracing.commandDetailLevel()).thenReturn(CommandDetailLevel.FULL);
      var config = mock(MongoConfig.class);
      when(config.tracing()).thenReturn(tracing);
      var listener = new MongoTracingCommandListener(telemetry, config, mock(OTelRuntimeConfig.class));
      var connection = new ConnectionDescription(new ServerId(new ClusterId(), new ServerAddress()));
      var command = BsonDocument.parse("{\"find\":\"token\",\"filter\":{\"owner\":\"user1\"}}");

      listener.commandStarted(
          new CommandStartedEvent(null, 1L, 1, connection, "token", "find", command));
      listener.commandSucceeded(
          new CommandSucceededEvent(
              null, 1L, 1, connection, "token", "find", new BsonDocument(), 100L));

      var captured = ArgumentCaptor.forClass(ReadableSpan.class);
      verify(recorder).onEnd(captured.capture());
      var attributes = captured.getValue().toSpanData().getAttributes();
      assertEquals("mongodb", attributes.get(AttributeKey.stringKey("db.system.name")));
      assertEquals("token", attributes.get(AttributeKey.stringKey("db.collection.name")));
      assertEquals(command.toJson(), attributes.get(AttributeKey.stringKey("db.query.text")));
    }
  }

  @Test
  void leavesOtherInstrumentationUnchanged() {
    try (var provider = SdkTracerProvider.builder().addSpanProcessor(new MongoDbSpanProcessor()).build()) {
      var span = provider.get("http-server").spanBuilder("GET /tokens").startSpan();
      span.end();
      var attributes = ((ReadableSpan) span).toSpanData().getAttributes();
      assertNull(attributes.get(AttributeKey.stringKey("db.system.name")));
    }
  }
}
