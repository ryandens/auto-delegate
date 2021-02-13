package com.github.ryandens.examples;

import com.github.ryandens.delegation.AutoDelegate;

import java.util.Collection;
import java.util.Set;

/**
 * Inspired by Effective Java Item 16's composition example. Joshua Bloch creates an abstract
 * "ForwardingSet" that can be re-used to make composition of behavior for {@link Set} APIs more
 * easy. {@link AutoDelegate} obviates the need for a common "Forwarding Set" implementation
 * allowing it to be generated for each {@link Set} implementation we want to write
 *
 * @param <E>
 */
@AutoDelegate(
    value = "inner",
    apisToDelegate = {Set.class})
public final class InstrumentedSet<E> extends AutoDelegate_InstrumentedSet<E> implements Set<E> {
  private int addCount;

  public InstrumentedSet(final Set<E> inner) {
    super(inner);
    this.addCount = 0;
  }

  @Override
  public boolean add(final E t) {
    addCount++;
    return super.add(t);
  }

  @Override
  public boolean addAll(final Collection<? extends E> c) {
    addCount += c.size();
    return super.addAll(c);
  }

  public int addCount() {
    return addCount;
  }
}
