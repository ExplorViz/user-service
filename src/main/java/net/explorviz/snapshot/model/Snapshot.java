package net.explorviz.snapshot.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;


@MongoEntity(collection = "snapshot")
public class Snapshot {

  /**
   * The creator of the snapshot.
   * Part-Identifier
   */
  private String owner;

  /**
   * The time the snapshot was created.
   * Part-Identifier
   */
  private Long createdAt;

  /**
   * The given name of the snapshot.
   */
  private String name;

  /**
   * The landscape token of the corresponding landscape.
   */
  private Document landscapeToken;

  /**
   * The structure data describing the landscape.
   */
  private Document structureData;

  /**
   * The configuration data describing the room.
   */
  private Document serializedRoom;

  /**
   * The timestamps of a room.
   */
  private Document timestamps;

  /**
   * The camera data describing position of the camera.
   */
  private Document camera;

  /**
   * The array of annotations of the landscape.
   */
  private Document annotations;

  /**
   * The identifier whether the snapshot is shared or not.
   */
  private boolean isShared;

  /**
   * Collection of users that subscribed the snapshot.
   */
  private Document subscribedUsers;

  /**
   * The optional value of the expiration date used for sharing.
   * Default value = 0
   */
  private Long deleteAt;

  /**
   * Just for testing.
   * TODO: Remove after implementation works.
   */
  private Document julius;

  @BsonCreator
  public Snapshot(@BsonProperty("owner") String owner, @BsonProperty("createdAt") Long createdAt,
      @BsonProperty("name") String name, @BsonProperty("landscapeToken") Document landscapeToken,
      @BsonProperty("structureData") Document structureData, @BsonProperty("serializedRoom") Document serializedRoom,
      @BsonProperty("timestamps") Document timestamps,
      @BsonProperty("camera") Document camera, @BsonProperty("annotations") Document annotations,
      @BsonProperty("isShared") boolean isShared, @BsonProperty("subscribedUsers") Document subscribedUsers,
      @BsonProperty("deleteAt") Long deleteAt, @BsonProperty("julius") Document julius) {
    this.owner = owner;
    this.createdAt = createdAt;
    this.name = name;
    this.landscapeToken = landscapeToken;
    this.structureData = structureData;
    this.serializedRoom = serializedRoom;
    this.timestamps = timestamps;
    this.camera = camera;
    this.annotations = annotations;
    this.isShared = isShared;
    this.subscribedUsers = subscribedUsers;
    this.deleteAt = deleteAt;
    this.julius = julius;
  }

  public Snapshot() {  }

  @BsonProperty("owner")
  public String getOwner() {
    return owner;
  }

  @BsonProperty("createdAt")
  public Long getCreatedAt() {
    return createdAt;
  }

  @BsonProperty("name")
  public String getName() {
    return name;
  }

  @BsonProperty("landscapeToken")
  public Document getLandscapeToken() {
    return landscapeToken;
  }

  @BsonProperty("structureData")
  public Document getStructureData() {
    return structureData;
  }

  @BsonProperty("serializedRoom")
  public Document getSerializedRoom() {
    return serializedRoom;
  }

  @BsonProperty("timestamps")
  public Document getTimestamps() {return timestamps;}

  @BsonProperty("camera")
  public Document getCamera() {
    return camera;
  }

  @BsonProperty("annotations")
  public Document getAnnotations() {
    return annotations;
  }

  @BsonProperty("isShared")
  public boolean getIsShared() {
    return isShared;
  }

  @BsonProperty("subscribedUsers")
  public Document getSubscribedUsers() {
    return subscribedUsers;
  }

  @BsonProperty("deleteAt")
  public Long getDeleteAt() {
    return deleteAt;
  }

  @BsonProperty("julius")
  public Document getJulius() {
    return julius;
  }

  @Override
  public String toString() {
    return getOwner() + " " + getCreatedAt() + " " + getName() + " " + getLandscapeToken() + " "
        + getLandscapeToken() + " " + getStructureData() + " " + getSerializedRoom() + " " + getTimestamps() + " "
        + getAnnotations() + " " + getIsShared() + " " + getSubscribedUsers() + " " + getDeleteAt() + " " + getJulius();
  }
}