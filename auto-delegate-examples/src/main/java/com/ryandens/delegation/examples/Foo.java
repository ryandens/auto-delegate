package com.ryandens.delegation.examples;

import com.ryandens.delegation.AutoDelegate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * Manages a thread-safe state {@link #hasBeenInvoked}. This first time {@link #a()} or {@link #d()}
 * is invoked, this boolean is flipped from {@link State#NOT_INVOKED} to either {@link
 * State#A_INVOKED} or {@link State#D_INVOKED}. If {@link #a()} is called first, every time {@link
 * #d()} is called an {@link IllegalStateException} wll be thrown. The opposite is true if {@link
 * #d()} is called first
 *
 * <p>Thread-safe
 */
@AutoDelegate(to = {Bar.class, Baz.class})
public final class Foo extends AutoDelegate_Foo implements Bar, Baz {

  private final AtomicReference<State> hasBeenInvoked;
  private final StateUpdaterFunction aInvokedUpdaterFunction;
  private final StateUpdaterFunction dInvokedUpdaterFunction;

  enum State {
    NOT_INVOKED,
    A_INVOKED,
    D_INVOKED
  }

  public Foo(final Bar bar, final Baz baz) {
    super(bar, baz);
    hasBeenInvoked = new AtomicReference<>(State.NOT_INVOKED);
    aInvokedUpdaterFunction = new StateUpdaterFunction(State.A_INVOKED);
    dInvokedUpdaterFunction = new StateUpdaterFunction(State.D_INVOKED);
  }

  @Override
  public boolean a() {
    final State previousValue = hasBeenInvoked.getAndUpdate(aInvokedUpdaterFunction);
    if (State.NOT_INVOKED.equals(previousValue) || State.A_INVOKED.equals(previousValue)) {
      return super.a();
    } else {
      throw new IllegalStateException("Can not call Foo.a() after a call to Foo.d()");
    }
  }

  @Override
  public Object d() {
    final State previousValue = hasBeenInvoked.getAndUpdate(dInvokedUpdaterFunction);
    if (State.NOT_INVOKED.equals(previousValue) || State.D_INVOKED.equals(previousValue)) {
      return super.d();
    } else {
      throw new IllegalStateException("Can not call Foo.d() after a call to Foo.a()");
    }
  }

  /**
   * A {@link UnaryOperator} that consumes a previous {@link State} and returns the specified {@link
   * #desiredNewState} if the previous state was either {@link State#NOT_INVOKED} or equal to the
   * desired new state. Otherwise, the previous state is returned.
   *
   * <p>For example, if the desired state is A_INVOKED but the previous state was B_INVOKED,
   * B_INVOKED will be returned so as to not change this value after being set by {@link
   * AtomicReference#getAndUpdate(UnaryOperator)}
   */
  private static final class StateUpdaterFunction implements UnaryOperator<State> {
    private final State desiredNewState;

    private StateUpdaterFunction(final State desiredNewState) {
      this.desiredNewState = desiredNewState;
    }

    @Override
    public State apply(final State previousState) {
      if (previousState.equals(State.NOT_INVOKED) || previousState.equals(desiredNewState)) {
        return desiredNewState;
      } else {
        return previousState;
      }
    }
  }
}
