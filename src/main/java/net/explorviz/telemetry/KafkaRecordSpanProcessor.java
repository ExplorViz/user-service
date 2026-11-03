package net.explorviz.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.smallrye.reactive.messaging.TracingMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/** Adds record content to the Kafka connector's existing producer spans. */
@ApplicationScoped
public class KafkaRecordSpanProcessor implements SpanProcessor {

  private static final ContextKey<Attributes> RECORD_CONTENT =
      ContextKey.named("explorviz-kafka-record-content");

  /** Carries record attributes to the connector without adding them to Kafka headers. */
  public static TracingMetadata metadataFor(String key, byte[] payload, String body) {
    var attributes =
        Attributes.builder()
            .put("messaging.kafka.message.key", key)
            .put("messaging.message.body.size", payload == null ? 0L : payload.length)
            .put("explorviz.messaging.tombstone", payload == null);
    if (payload != null) {
      attributes.put("messaging.message.body", body);
    }
    return TracingMetadata.withCurrent(Context.current().with(RECORD_CONTENT, attributes.build()));
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    var attributes = parentContext.get(RECORD_CONTENT);
    if (attributes != null
        && span.getKind() == SpanKind.PRODUCER
        && "io.smallrye.reactive.messaging".equals(span.getInstrumentationScopeInfo().getName())) {
      span.setAllAttributes(attributes);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // Record attributes are set before the connector ends the span.
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
