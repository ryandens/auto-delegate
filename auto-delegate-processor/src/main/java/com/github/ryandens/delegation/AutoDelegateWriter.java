package com.github.ryandens.delegation;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.List;

final class AutoDelegateWriter {
  private final Filer filer;
  private final String destinationPackage;
  private final String className;
  private final String fieldName;
  private final List<Class<?>> apisToDelegate;
  private final TypeMirror declaringType;

  AutoDelegateWriter(
      final Filer filer,
      final String destinationPackage,
      final String className,
      final String fieldName,
      final List<Class<?>> apisToDelegate,
      final TypeMirror declaringType) {
    this.filer = filer;
    this.destinationPackage = destinationPackage;
    this.className = className;
    this.fieldName = fieldName;
    this.apisToDelegate = apisToDelegate;
    this.declaringType = declaringType;
  }

  void write() {
    final var autoDelegator =
        TypeSpec.classBuilder(className)
            .addJavadoc("TODO")
            .addTypeVariable(TypeVariableName.get("T"))
            .superclass(ParameterizedTypeName.get(declaringType))
//                .addMethod(MethodSpec.overriding().build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .build();
    final var javaFile = JavaFile.builder(destinationPackage, autoDelegator).build();
    try {
      javaFile.writeTo(filer);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Problem writing " + destinationPackage + "." + className + " class to file", e);
    }
  }
}
