package com.github.ryandens.examples;

import com.github.ryandens.delegation.AutoDelegate;

import java.util.Set;

/**
 * Inspired by Effective Java Item 16's composition example. Joshua Bloch creates an abstract
 * "ForwardingSet" that can be re-used to make composition of behavior for {@link Set} APIs more
 * easy. {@link AutoDelegate} obviates the need for a common "Forwarding Set" implementation
 * allowing it to be generated for each {@link Set} implementation we want to write
 *
 * @param <T>
 */
@AutoDelegate(
    value = "inner",
    apisToDelegate = {Set.class})
public abstract class InstrumentedSet<T> implements Set<T> {
  private final Set<T> inner;
  private int addCount;

  public InstrumentedSet(final Set<T> inner) {
    this.inner = inner;
    this.addCount = 0;
  }

  @Override
  public boolean add(final T t) {
    addCount++;
    return inner.add(t);
  }
}
