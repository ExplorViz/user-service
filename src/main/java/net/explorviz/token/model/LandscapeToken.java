package net.explorviz.token.model;

import com.google.common.base.Objects;

/**
 * Represents a landscape token.
 */
public class LandscapeToken {

  /**
   * The actual token value.
   */
  final private String value;

  /**
   * The id of the user owning this token.
   */
  final private String ownerId;

  public LandscapeToken(final String value, final String ownerId) {
    this.value = value;
    this.ownerId = ownerId;
  }

  public String getValue() {
    return value;
  }

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


}
