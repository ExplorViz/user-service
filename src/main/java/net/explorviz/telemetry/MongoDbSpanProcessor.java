package net.explorviz.telemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/** Supplies the database system identity missing from Quarkus MongoDB command spans. */
@ApplicationScoped
public class MongoDbSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    if ("quarkus-mongodb-client".equals(span.getInstrumentationScopeInfo().getName())) {
      span.setAttribute("db.system.name", "mongodb");
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // All enrichment happens while the span is writable in onStart.
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
