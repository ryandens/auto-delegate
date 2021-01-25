package com.github.ryandens.delegation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Decorates an abstract class (or interface?) with metadata to build an */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoDelegate {

  /** @return the name of the field to delegate to */
  String value();

  /**
   * @return an array of interfaces that the generated class should delegate to. Note, for each
   *     {@link Class} provided in this array, {@link Class#isAssignableFrom(Class)} must be true
   *     where the parameterized class is the type of the field denoted by {@link #value()}
   */
  Class<?>[] apisToDelegate();
}
