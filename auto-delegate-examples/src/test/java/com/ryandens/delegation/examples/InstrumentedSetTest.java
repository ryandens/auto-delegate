package com.ryandens.delegation.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstrumentedSet} */
final class InstrumentedSetTest {

  private InstrumentedSet<String> instrumentedSet;

  @BeforeEach
  void beforeEach() {
    // GIVEN an instrumented Set<String>
    instrumentedSet = new InstrumentedSet<>(new HashSet<>());
  }

  @Test
  void testInitialAddCount() {
    // VERIFY initial addCount is 0
    assertEquals(0, instrumentedSet.addCount());
  }

  /** Tests that the {@link InstrumentedSet#add} implementation functions properly */
  @Test
  void testAdd() {
    // WHEN we add an element to the Set
    instrumentedSet.add("foo");
    // VERIFY the addCount is now 1
    assertEquals(1, instrumentedSet.addCount());
    // WHEN we add an element to the Set again
    instrumentedSet.add("bar");
    // VERIFY the addCount is now 2
    assertEquals(2, instrumentedSet.addCount());
    // WHEN we add an element to the Set again that is already in the Set
    instrumentedSet.add("foo");
    // VERIFY the addCount is now 3
    assertEquals(3, instrumentedSet.addCount());
    // VERIFY size is 2
    assertEquals(2, instrumentedSet.size());
  }

  /** Tests that the {@link InstrumentedSet#addAll} implementation functions properly */
  @Test
  void testAddAll() {
    // WHEN we use addAll to add two elements to the Set
    instrumentedSet.addAll(Set.of("foo", "bar"));
    // VERIFY the addCount is now 2
    assertEquals(2, instrumentedSet.addCount());
  }
}
