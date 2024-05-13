package net.explorviz.userapi.model;

import com.mongodb.lang.Nullable;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity
public class UserAPI {

  /**
   * The user id given by auth0.
   * Builds 'identifier' with token.
   */
  private String uId;

  /**
   * The name of the corresponding API token.
   */
  private String name;

  /**
   * The actual API token.
   * Builds 'uId' with token.
   */
  private String token;

  /**
   * The value of the creation date.
   */
  private long createdAt;

  /**
   * The optional value of the expiration date.
   */
  @Nullable
  private Long expires;

  /**
   * Token for access to a software landscape.
   *
   * @param uId       The user id given by auth0.
   * @param name      The name of the corresponding API token.
   * @param token     Structure of API token, creation date and corresponding expiration date.
   */
  @BsonCreator
  public UserAPI(@BsonProperty("uId") String uId,
      @BsonProperty("name") String name,
      @BsonProperty("token") String token,
      @BsonProperty("createdAt") long createdAt,
      @Nullable @BsonProperty("expires") Long expires) {
    this.uId = uId;
    this.name = name;
    this.token = token;
    this.createdAt = createdAt;
    this.expires = expires;
  }

  /**
   * User id of the token owner.
   *
   * @return Owner of the token
   */
  @BsonProperty
  public String getuId() { return this.uId; }

  /**
   * Name of the token.
   *
   * @return Name of the token
   */
  @BsonProperty
  public String getName() { return this.name; }

  /**
   * The API token value.
   *
   * @return API token
   */
  @BsonProperty
  public String getToken() { return this.token; }

  /**
   * The numeric value of the creation date.
   *
   * @return numeric creation date
   */
  @BsonProperty
  public long getCreatedAt() { return this.createdAt; }

  /**
   * The numeric value of the deletion date.
   *
   * @return numeric deletion date
   */
  @BsonProperty
  @Nullable
  public Long getExpires() { return expires; }

  /**
   * Serializes the user API to the corresponding avro model.
   *
   * @return avro representation of this user API
   */
  public net.explorviz.avro.UserAPI toAvro() {
    if (this.expires == null){
      return net.explorviz.avro.UserAPI.newBuilder().setUId(this.uId)
          .setName(this.name).setToken(this.token).setCreatedAt(this.createdAt).build();
    } else {
      return net.explorviz.avro.UserAPI.newBuilder().setUId(this.uId)
          .setName(this.name).setToken(this.token).setCreatedAt(this.createdAt)
          .setExpires(this.expires).build();
    }
  }
}
