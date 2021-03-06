package com.ryandens.delegation.examples;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Foo} */
final class FooTest {

  private Foo foo;

  @BeforeEach
  void beforeEach() {
    // GIVEN a valid foo instance
    final var bar = mock(Bar.class);
    final var baz = mock(Baz.class);
    foo = new Foo(bar, baz);
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
  }
}
