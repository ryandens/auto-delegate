package com.ryandens.delegation.examples;

import com.ryandens.delegation.AutoDelegate;

/** Base interface. */
interface Base {
  String foo();
}

/** Extension of {@link Base}. */
interface Extension extends Base {
  String bar();
}

/**
 * Implementation of {@link Extension} that delegates methods from {@link Base} to some other
 * instance.
 */
@AutoDelegate(Base.class)
class ExtensionImpl extends AutoDelegate_ExtensionImpl implements Extension {

  ExtensionImpl(final Base delegate) {
    super(delegate);
  }

  @Override
  public String bar() {
    return "bar";
  }
}
