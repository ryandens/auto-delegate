package com.ryandens.delegation.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstrumentedMapTest {
  private InstrumentedMap<String, String> instrumentedMap;

  @BeforeEach
  void beforeEach() {
    // GIVEN an instrumented Map<String>
    instrumentedMap = new InstrumentedMap<>(new HashMap<>());
  }

  @Test
  void testInitialPutCount() {
    // VERIFY initial putCount is 0
    assertEquals(0, instrumentedMap.putCount());
  }

  /** Tests that the {@link InstrumentedMap#put} implementation functions properly */
  @Test
  void testPut() {
    // WHEN we add an element to the Map
    instrumentedMap.put("foo", "!");
    // VERIFY the putCount is now 1
    assertEquals(1, instrumentedMap.putCount());
    // WHEN we add an element to the Map again
    instrumentedMap.put("bar", "!!");
    // VERIFY the putCount is now 2
    assertEquals(2, instrumentedMap.putCount());
    // WHEN we add an element to the Map again that is already in the Map
    instrumentedMap.put("foo", "!!!");
    // VERIFY the putCount is now 3
    assertEquals(3, instrumentedMap.putCount());
    // VERIFY size is 2
    assertEquals(2, instrumentedMap.size());
  }

  /** Tests that the {@link InstrumentedMap#putAll} implementation functions properly */
  @Test
  void testPutALl() {
    // WHEN we use putAll to add two elements to the Map
    instrumentedMap.putAll(Map.of("k1", "v1", "k2", "v2"));
    // VERIFY the putCount is now 2
    assertEquals(2, instrumentedMap.putCount());
  }
}
