package net.explorviz.token.resources.filter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

/**
 * HTTP handler methods marked with this annotation can only be accessed by the user that owns the
 * associated resource.
 */
@Target({TYPE, METHOD})
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceOwnership {

  /**
   * Set user ID by default to empty string when authorization is disabled.
   */
  String uidField() default "";
}
