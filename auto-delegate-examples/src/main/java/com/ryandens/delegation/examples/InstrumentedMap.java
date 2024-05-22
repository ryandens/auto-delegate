package com.ryandens.delegation.examples;

import com.ryandens.delegation.AutoDelegate;
import java.util.Map;

@AutoDelegate(Map.class)
public final class InstrumentedMap<K, V> extends AutoDelegate_InstrumentedMap<K, V>
    implements Map<K, V> {
  private int putCount;

  InstrumentedMap(final Map<K, V> inner0) {
    super(inner0);
  }

  @Override
  public V put(final K k, final V v) {
    putCount++;
    return super.put(k, v);
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    putCount += m.size();
    super.putAll(m);
  }

  public int putCount() {
    return putCount;
  }
}
