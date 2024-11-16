package net.explorviz.token.model;

import com.google.common.base.Objects;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Represents a landscape token.
 */
@MongoEntity(collection = "token")
public class LandscapeToken {


  /**
   * The actual token value.
   */
  private String value;

  // Property should unique, panache mongodb does not yet
  // support indices: https://github.com/quarkusio/quarkus/issues/9801


  private String secret;

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
   * Users that may access this token.
   */
  private List<String> sharedUsersIds;

  /**
   * Token for access to a software landscape.
   *
   * @param value       The actual token value.
   * @param secret      Secret string, which together with "value" is used to check authorization.
   * @param ownerId     Id of the user who generates the token.
   * @param created     Timestamp which indicates when the token was created.
   * @param alias       Used-defined alias to easily identify a token.
   * @param sharedUsers Users who have access to this token.
   */
  @BsonCreator
  public LandscapeToken(@BsonProperty("value") final String value,
      @BsonProperty("secret") final String secret, @BsonProperty("owner") final String ownerId,
      @BsonProperty("created") final long created, @BsonProperty("alias") final String alias,
      @BsonProperty("sharedUsers") final List<String> sharedUsers) {
    this.value = value;
    this.ownerId = ownerId;
    this.created = created;
    this.alias = alias;
    this.secret = secret;
    this.sharedUsersIds = sharedUsers;
  }

  public LandscapeToken(final String value, final String secret, final String ownerId,
      final long created, final String alias) {
    this(value, secret, ownerId, created, alias, new ArrayList<>());
  }

  public LandscapeToken() { /* Jackson */
  }

  /**
   * The actual token, that uniquely identifies a landscape.
   *
   * @return the token
   */
  @BsonProperty("value")
  public String getValue() {
    return this.value;
  }

  /**
   * Id of the owner of the landscape token, i.e., the user that created it.
   *
   * @return Owner user-id of the landscape
   */
  @BsonProperty("owner")
  public String getOwnerId() {
    return this.ownerId;
  }

  /**
   * The timestamp the token was created.
   *
   * @return timestamp (epoch) when the landscape was created
   */
  @BsonProperty("created")
  public long getCreated() {
    return this.created;
  }

  /**
   * An alias of the token than was specified by the user upon creation.
   *
   * @return Human-friendly alias, specified by the user at creation
   */
  @BsonProperty("alias")
  public String getAlias() {
    return this.alias;
  }


  /**
   * The secret of the token that is required to write spans to it.
   *
   * @return the secret required to write to the landscape
   */
  @BsonProperty("secret")
  public String getSecret() {
    return this.secret;
  }

  @BsonProperty("sharedUsers")
  public List<String> getSharedUsersIds() {
    return this.sharedUsersIds;

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
    return Objects.equal(this.value, token.value) && Objects.equal(this.ownerId, token.ownerId)
        && Objects.equal(this.created, token.created) && Objects.equal(this.alias, token.alias)
        && Objects.equal(this.secret, token.secret) && Objects.equal(this.sharedUsersIds,
        token.sharedUsersIds);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.value, this.ownerId);
  }

  /**
   * Serializes the token to the corresponding avro model.
   *
   * @return avro representation of this token
   */
  public net.explorviz.avro.LandscapeToken toAvro() {
    return net.explorviz.avro.LandscapeToken.newBuilder().setValue(this.value)
        .setSecret(this.secret).setOwnerId(this.ownerId).setAlias(this.alias)
        .setCreated(this.getCreated()).build();
  }

}
