package net.explorviz.token.model;

import com.google.common.base.Objects;
import io.quarkus.mongodb.panache.MongoEntity;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonCreator;
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

  // Property should unique, panache mongodb does not yet
  // support indices: https://github.com/quarkusio/quarkus/issues/9801


  /**
   * The id of the user owning this token.
   */
  private String ownerId;

  /**
   * Timestamp when the token was created.
   */
  private long created;

  /**
   * User defined alias.
   */
  private String alias;

  /**
   *  Users that may access this token.
   */
  private List<String> sharedUsersIds;

  @BsonCreator
  public LandscapeToken(@BsonProperty("value") final String value,
                        @BsonProperty("owner") final String ownerId,
                        @BsonProperty("created") final long created,
                        @BsonProperty("alias") final String alias,
                        @BsonProperty("sharedUsers") final List<String> sharedUsers) {
    this.value = value;
    this.ownerId = ownerId;
    this.created = created;
    this.alias = alias;
    this.sharedUsersIds = sharedUsers;
  }

  public LandscapeToken(final String value,
                        final String ownerId,
                        final long created,
                        final String alias) {
    this.value = value;
    this.ownerId = ownerId;
    this.created = created;
    this.alias = alias;
    this.sharedUsersIds = List.of();
  }

  public LandscapeToken() { /* Jackson */ }

  @BsonProperty("value")
  public String getValue() {
    return this.value;
  }

  @BsonProperty("owner")
  public String getOwnerId() {
    return this.ownerId;
  }

  @BsonProperty("created")
  public long getCreated() {
    return created;
  }

  @BsonProperty("alias")
  public String getAlias() {
    return alias;
  }

  @BsonProperty("sharedUsers")
  public List<String> getSharedUsersIds() {
    return sharedUsersIds;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    final LandscapeToken token = (LandscapeToken) o;
    return Objects.equal(this.value, token.value)
        && Objects.equal(this.ownerId, token.ownerId)
        && Objects.equal(this.created, token.created)
        && Objects.equal(this.alias, token.alias)
        && Objects.equal(this.sharedUsersIds, token.sharedUsersIds);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.value, this.ownerId);
  }

}
