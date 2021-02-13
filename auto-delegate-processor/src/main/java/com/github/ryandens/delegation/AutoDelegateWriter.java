package com.github.ryandens.delegation;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.stream.Collectors;

final class AutoDelegateWriter {
  private final Filer filer;
  private final String destinationPackage;
  private final String className;
  private final Set<ExecutableElement> apisToDelegate;
  private final TypeMirror innerType;

  AutoDelegateWriter(
      final Filer filer,
      final String destinationPackage,
      final String className,
      final Set<ExecutableElement> apisToDelegate,
      final TypeMirror innerType) {
    this.filer = filer;
    this.destinationPackage = destinationPackage;
    this.className = className;
    this.apisToDelegate = apisToDelegate;
    this.innerType = innerType;
  }

  void write() {

    final var methodSpecs =
        apisToDelegate.stream()
            .map(
                executableElement -> {
                  final var parameters =
                      executableElement.getParameters().stream()
                          .map(parameter -> parameter.getSimpleName().toString())
                          .reduce((s, s2) -> s + "," + s2)
                          .orElse("");

                  final String returnPrefix;
                  if (TypeKind.VOID.equals(executableElement.getReturnType().getKind())) {
                    returnPrefix = "";
                  } else {
                    returnPrefix = "return ";
                  }

                  return MethodSpec.overriding(executableElement)
                      .addCode(
                          CodeBlock.builder()
                              .addStatement(
                                  returnPrefix
                                      + "inner."
                                      + executableElement.getSimpleName().toString()
                                      + "("
                                      + parameters
                                      + ")")
                              .build())
                      .build();
                })
            .collect(Collectors.toSet());

    final var autoDelegator =
        TypeSpec.classBuilder(className)
            .addJavadoc("TODO")
            .addSuperinterface(innerType)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addParameter(TypeName.get(innerType), "inner", Modifier.FINAL)
                    .addCode(CodeBlock.builder().add("this.inner = inner;").build())
                    .build())
            .addTypeVariable(TypeVariableName.get("E"))
            .addMethods(methodSpecs)
            .addField(
                FieldSpec.builder(
                        TypeName.get(innerType), "inner", Modifier.FINAL, Modifier.PRIVATE)
                    .build())
            .addModifiers(Modifier.ABSTRACT)
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
