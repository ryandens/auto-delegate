package com.ryandens.delegation.examples;

public interface Baz {

  Object d();

  long f();

  default String g() {
    return "g";
  }
}
