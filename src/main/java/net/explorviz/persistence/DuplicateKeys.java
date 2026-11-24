package net.explorviz.persistence;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** Detection and reporting of violated unique indexes. */
public final class DuplicateKeys {

  private DuplicateKeys() {}

  public static boolean isDuplicateKey(final MongoException e) {
    return ErrorCategory.fromErrorCode(e.getCode()) == ErrorCategory.DUPLICATE_KEY;
  }

  /**
   * Creates the response for attempts to create something that already exists.
   *
   * @param message the message to be shown to the user
   * @return a response with status 409
   */
  public static Response conflict(final String message) {
    return Response.status(Response.Status.CONFLICT)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(Map.of("message", message))
        .build();
  }
}
