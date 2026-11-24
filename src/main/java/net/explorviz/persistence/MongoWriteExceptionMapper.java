package net.explorviz.persistence;

import com.mongodb.MongoWriteException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reports violated unique indexes, e.g. due to concurrent requests, as conflicts. */
@Provider
public class MongoWriteExceptionMapper implements ExceptionMapper<MongoWriteException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MongoWriteExceptionMapper.class);

  @Override
  public Response toResponse(final MongoWriteException e) {
    if (DuplicateKeys.isDuplicateKey(e)) {
      return DuplicateKeys.conflict("An entry with the same identifier already exists.");
    }
    LOGGER.error("Writing to the database failed", e);
    return Response.serverError().build();
  }
}
