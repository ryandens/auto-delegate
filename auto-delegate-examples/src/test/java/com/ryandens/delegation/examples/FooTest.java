package com.ryandens.delegation.examples;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Foo} */
final class FooTest {

  private Foo foo;
  private Baz innerComposedBaz;

  @BeforeEach
  void beforeEach() {
    // GIVEN a valid foo instance
    final var bar = mock(Bar.class);
    innerComposedBaz = mock(Baz.class);
    foo = new Foo(bar, innerComposedBaz);
  }

  /** Verifies that if we invoke {@link Foo#a()} */
  @Test
  void test_foo_a_invoked_first() {
    // WHEN we invoke foo.a()
    foo.a();
    // VERIFY foo.d() now causes exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.d();
        });

    // VERIFY foo.d() still causes an exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.d();
        });

    // VERIFY we can still re-invoke foo.a()
    foo.a();

    // VERIFY foo.d() still causes an exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.d();
        });
  }

  /** TODO */
  @Test
  void test_foo_d_invoked_first() {
    // WHEN we invoke foo.d()
    foo.d();
    // VERIFY foo.a() now causes exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.a();
        });

    // VERIFY foo.a() still causes an exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.a();
        });

    // VERIFY we can still re-invoke foo.d()
    foo.d();
    // VERIFY foo.a() still causes an exception
    assertThrows(
        IllegalStateException.class,
        () -> {
          foo.a();
        });

    // WHEN we invoke foo.g(), which has a default implementation defined on the interface Baz
    final String g = foo.g();
    // VERIFY the composed instance baz was delegated to, not the default implementation of the API
    // inherited by Foo
    verify(innerComposedBaz, times(1)).g();
    // VERIFY g is null, what the mock implementation of Baz.g returns, rather than the string "g"
    // what the default implementation of Baz.g returns
    assertNull(g);
  }
}
