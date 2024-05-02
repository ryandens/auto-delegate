package com.ryandens.delegation.examples;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstrumentedMapTest {
    private InstrumentedMap<String, String> instrumentedMap;

    @BeforeEach
    void beforeEach() {
        // GIVEN an instrumented Set<String>
        instrumentedMap = new InstrumentedMap<>(new HashMap<>());
    }

    @Test
    void testInitialAddCount() {
        // VERIFY initial addCount is 0
        assertEquals(0, instrumentedMap.putCount());
    }

    /**
     * Tests that the {@link InstrumentedSet#add} implementation functions properly
     */
    @Test
    void testPut() {
        // WHEN we add an element to the Set
        instrumentedMap.put("foo", "!");
        // VERIFY the addCount is now 1
        assertEquals(1, instrumentedMap.putCount());
        // WHEN we add an element to the Set again
        instrumentedMap.put("bar", "!!");
        // VERIFY the addCount is now 2
        assertEquals(2, instrumentedMap.putCount());
        // WHEN we add an element to the Set again that is already in the Set
        instrumentedMap.put("foo", "!!!");
        // VERIFY the addCount is now 3
        assertEquals(3, instrumentedMap.putCount());
        // VERIFY size is 2
        assertEquals(2, instrumentedMap.size());
    }

    /**
     * Tests that the {@link InstrumentedSet#addAll} implementation functions properly
     */
    @Test
    void testPutALl() {
        // WHEN we use addAll to add two elements to the Set
        instrumentedMap.putAll(Map.of("k1", "v1", "k2", "v2"));
        // VERIFY the addCount is now 2
        assertEquals(2, instrumentedMap.putCount());
    }
}
