package net.explorviz.userapi.model;

import com.mongodb.lang.Nullable;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity(collection = "userapi")
public class UserApi {

  /**
   * The user id given by auth0.
   * Builds 'identifier' with token.
   */
  private String uid;

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
   * The host url.
   */
  private String hostUrl;

  /**
   * The value of the creation date.
   */
  private Long createdAt;

  /**
   * The optional value of the expiration date.
   * Default value = 0
   */
  private Long expires;

  /**
   * UserAPI entrie for one API token.
   *
   * @param uid       The user id given by auth0.
   * @param name      The name of the corresponding API token.
   * @param token     The API token.
   */
  @BsonCreator
  public UserApi(@BsonProperty("uid") String uid,
      @BsonProperty("name") String name,
      @BsonProperty("token") String token,
      @BsonProperty("hostUrl") String hostUrl,
      @BsonProperty("createdAt") long createdAt,
      @BsonProperty("expires") Long expires) {
    this.uid = uid;
    this.name = name;
    this.token = token;
    this.hostUrl = hostUrl;
    this.createdAt = createdAt;
    this.expires = expires;
  }

  public UserApi() {}

  /**
   * User id of the token owner.
   *
   * @return Owner of the token
   */
  @BsonProperty("uid")
  public String getUid() {
    return this.uid;
  }

  /**
   * Name of the token.
   *
   * @return Name of the token
   */
  @BsonProperty("name")
  public String getName() {
    return this.name;
  }

  /**
   * The API token value.
   *
   * @return API token
   */
  @BsonProperty("token")
  public String getToken() {
    return this.token;
  }

  /**
   * The host url.
   *
   * @return host url
   */
  @BsonProperty("hostUrl")
  public String getHostUrl() {
    return this.hostUrl;
  }

  /**
   * The numeric value of the creation date.
   *
   * @return numeric creation date
   */
  @BsonProperty("createdAt")
  public Long getCreatedAt() {
    return this.createdAt;
  }

  /**
   * The numeric value of the deletion date.
   *
   * @return numeric deletion date
   */
  @BsonProperty("expires")
  @Nullable
  public Long getExpires() {
    return this.expires;
  }

}
