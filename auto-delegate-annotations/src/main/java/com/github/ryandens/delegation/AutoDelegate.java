package com.github.ryandens.delegation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Decorates an abstract class (or interface?) with metadata to build an */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoDelegate {

  /**
   * @return an array of interfaces that the generated class should delegate to. Note, for each
   *     {@link Class} provided in this array, the class annotated by this {@link AutoDelegate}
   *     element must be assignable from it (e.g. {@link Class#isAssignableFrom(Class)} must be true
   *     because the annotated {@link Class} implements the {@link Class} specified in this array
   */
  Class<?>[] apisToDelegate();
}
