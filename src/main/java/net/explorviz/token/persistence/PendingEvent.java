package net.explorviz.token.persistence;

import java.util.UUID;
import net.explorviz.proto.EventType;
import org.bson.Document;

/**
 * An event of a token that has not been published yet.
 *
 * @param id identifies the event among all events of the token
 * @param type type of the event
 * @param userId the user whose access changed, or {@code null}
 * @param clonedTokenId the token that was cloned, or {@code null}
 */
public record PendingEvent(String id, EventType type, String userId, String clonedTokenId) {

  public static PendingEvent of(final EventType type) {
    return new PendingEvent(UUID.randomUUID().toString(), type, null, null);
  }

  public static PendingEvent ofAccess(final EventType type, final String userId) {
    return new PendingEvent(UUID.randomUUID().toString(), type, userId, null);
  }

  public static PendingEvent ofClone(final String clonedTokenId) {
    return new PendingEvent(
        UUID.randomUUID().toString(), EventType.EVENT_TYPE_CLONED, null, clonedTokenId);
  }

  /* default */ Document toDocument() {
    return new Document("id", this.id)
        .append("type", this.type.name())
        .append("userId", this.userId)
        .append("clonedTokenId", this.clonedTokenId);
  }

  /* default */ static PendingEvent fromDocument(final Document document) {
    return new PendingEvent(
        document.getString("id"),
        EventType.valueOf(document.getString("type")),
        document.getString("userId"),
        document.getString("clonedTokenId"));
  }
}
