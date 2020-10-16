package net.explorviz.token.model;

import com.google.common.base.Objects;
import io.quarkus.mongodb.panache.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Represents a landscape token.
 */
@MongoEntity
public class LandscapeToken {

  /**
   * The actual token value.
   */
  private String value;

  /**
   * The id of the user owning this token.
   */
  private String ownerId;

  @BsonCreator
  public LandscapeToken(@BsonProperty("value") final String value,
                        @BsonProperty("owner") final String ownerId) {
    this.value = value;
    this.ownerId = ownerId;
  }

  public LandscapeToken() { /*Jackson*/ }

  @BsonProperty("value")
  public String getValue() {
    return value;
  }


  @BsonProperty("owner")
  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final LandscapeToken token = (LandscapeToken) o;
    return Objects.equal(value, token.value) &&
        Objects.equal(ownerId, token.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, ownerId);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
