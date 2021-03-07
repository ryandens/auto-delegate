package com.ryandens.delegation;

import java.util.Objects;
import javax.lang.model.type.DeclaredType;

/** Immutable value class encapsulating all the information about an inner type to delegate to */
final class DelegationTargetDescriptor {

  private final DeclaredType declaredType;
  private final String fieldName;

  DelegationTargetDescriptor(final DeclaredType declaredType, final String fieldName) {
    this.declaredType = Objects.requireNonNull(declaredType);
    this.fieldName = Objects.requireNonNull(fieldName);
  }

  DeclaredType declaredType() {
    return declaredType;
  }

  String fieldName() {
    return fieldName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final DelegationTargetDescriptor that = (DelegationTargetDescriptor) o;
    return declaredType.equals(that.declaredType) && fieldName.equals(that.fieldName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(declaredType, fieldName);
  }

  @Override
  public String toString() {
    return "DelegationTargetDescriptor{"
        + "declaredType="
        + declaredType
        + ", fieldName='"
        + fieldName
        + '\''
        + '}';
  }
}
