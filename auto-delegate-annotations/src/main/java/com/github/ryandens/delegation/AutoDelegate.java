package com.github.ryandens.delegation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Decorates a class with metadata to describe an abstract parent class that automatically delegates
 * to an inner composed instance of an interface. This annotation processor is inspired by the
 * Kotlin language feature <a href="https://kotlinlang.org/docs/delegation.html">delegation</a>.
 *
 * <p>The goal of this is to encourage the use of composition over inheritance as described by
 * Effective Java Item 18 "Favor composition over inheritance". In the section of the book, Bloch
 * describes an {@code InstrumentedSet} that counts the number of items added to it. In order to
 * accomplish this, Bloch creates an abstract implementation of {@link java.util.Set} called {@code
 * ForwardingSet} that simply composes a {@link java.util.Set} instance and forwards all calls to
 * it. This allows Bloch to write the {@code InstrumentedSet} in a less verbose manner, by extending
 * {@code ForwardingSet} and overriding the "add" related methods for instrumentation purposes. This
 * is a great solution in the context of Java, but Kotlin lowers the cognitive barrier of using
 * composition by making it less verbose to do so. In Kotlin, the need for a {@code ForwardingSet}
 * is obviated by the "delegation" language feature linked above. The {@code InstrumentedSet} can be
 * written concisely without relying on writing a {@code ForwardingSet} like:
 *
 * <pre>{@code
 * class InstrumentedSet<E>(val inner: MutableSet<E>) : MutableSet<E> by inner {
 *     var count: Int = 0
 *
 *     override fun add(element: E): Boolean {
 *         count++
 *         return inner.add(element)
 *     }
 *
 *     override fun addAll(elements: Collection<E>) : Boolean {
 *         count += elements.size
 *         return inner.addAll(elements)
 *     }
 * }
 * }</pre>
 *
 * This annotation strives to enable developers in the same fashion by generating abstract {@code
 * Forwarding*} classes that delegate to the inner composed instance. An equivalent {@code
 * InstrumentedSet} implementation written with {@code AutoDelegate} is
 *
 * <pre>{@code
 * @AutoDelegate(Set.class)
 * public final class InstrumentedSet<E> extends AutoDelegate_InstrumentedSet<E> implements Set<E> {
 *   private int addCount;
 *
 *   public InstrumentedSet(final Set<E> inner) {
 *     super(inner);
 *     this.addCount = 0;
 *   }
 *
 *   @Override
 *   public boolean add(final E t) {
 *     addCount++;
 *     return super.add(t);
 *   }
 *
 *   @Override
 *   public boolean addAll(final Collection<? extends E> c) {
 *     addCount += c.size();
 *     return super.addAll(c);
 *   }
 *
 *   public int addCount() {
 *     return addCount;
 *   }
 * }
 * }</pre>
 *
 * While this is not as concise as the Kotlin implementation, it generates a class called {@code
 * AutoDelegate_InstrumentedSet} in the same package as the declaring class. The declared class can
 * then extend the generated class and call {code super} APIs where appropriate, only overriding
 * methods that are relevant to the implementation
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoDelegate {

  /**
   * @return an interface that the generated class should delegate to via an inner composed
   *     instance. Note,the class annotated by this {@link AutoDelegate} element must be assignable
   *     from the class provided in this annotation.
   */
  Class<?> value();
}
