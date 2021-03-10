package net.explorviz.token.resources.filter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

@Target({TYPE, METHOD})
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceOwnership {
  String uidField() default "";
}
