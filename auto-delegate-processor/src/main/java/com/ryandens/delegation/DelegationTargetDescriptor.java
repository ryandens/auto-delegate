package com.ryandens.delegation;

import javax.lang.model.type.DeclaredType;

/** Immutable value class encapsulating all the information about an inner type to delegate to */
final class DelegationTargetDescriptor {

  private final DeclaredType declaredType;
  private final String fieldName;

  DelegationTargetDescriptor(final DeclaredType declaredType, final String fieldName) {
    this.declaredType = declaredType;
    this.fieldName = fieldName;
  }

  DeclaredType declaredType() {
    return declaredType;
  }

  String fieldName() {
    return fieldName;
  }
}
