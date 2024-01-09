package com.ryandens.delegation.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Test that verifies that the partial delegation example works as expected. */
final class PartialDelegationExampleTest {

  @Test
  void partial_delegation() {
    final var delegate =
        new Base() {
          @Override
          public String foo() {
            return "foo";
          }
        };
    final var extension = new ExtensionImpl(delegate);
    assertEquals("bar", extension.bar());
  }
}
