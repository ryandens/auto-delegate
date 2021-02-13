package com.github.ryandens.delegation;

import com.google.auto.common.MoreTypes;
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

/**
 * Uses {@link com.squareup.javapoet} to write the class file that can be used to {@link
 * AutoDelegate} APIs
 */
final class AutoDelegateWriter {
  private final Filer filer;
  private final String destinationPackage;
  private final String className;
  private final Set<ExecutableElement> apisToDelegate;
  private final TypeMirror innerType;

  /**
   * @param filer for writing generated source code to the local environment
   * @param destinationPackage where the Java class should be written to
   * @param className of the generated Java class
   * @param apisToDelegate abstract APIs that should automatically delegate to an inner composed
   *     instance
   * @param innerType the {@link TypeMirror} of the composed instance. This Type must have apis that
   *     match all elements in {@link #apisToDelegate}
   */
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

    final var innerField =
        FieldSpec.builder(TypeName.get(innerType), "inner", Modifier.FINAL, Modifier.PRIVATE)
            .build();
    // Get the TypeVariables from the innerType
    final var typeVariables =
        MoreTypes.asTypeElement(innerType).getTypeParameters().stream()
            .map(TypeVariableName::get)
            .collect(Collectors.toSet());
    // creates a MethodSpec for the constructor that takes an instance of type innerType and
    // assigns it to a field with name "inner"
    final var constructor =
        MethodSpec.constructorBuilder()
            .addParameter(TypeName.get(innerType), "inner", Modifier.FINAL)
            .addCode(CodeBlock.builder().add("this.inner = inner;").build())
            .build();
    // create MethodSpecs for the apisToDelegate
    final Set<MethodSpec> methodSpecs = delegatingMethodSpecs();

    // creates a TypeSpec for an abstract auto-delegating implementation of innerType
    final var autoDelegator =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.ABSTRACT)
            .addJavadoc(
                "Shallowly immutable, shallowly thread-safe abstract class that forwards to an inner composed {@link "
                    + innerType.toString()
                    + "}")
            .addSuperinterface(innerType)
            .addTypeVariables(typeVariables)
            .addField(innerField)
            .addMethod(constructor)
            .addMethods(methodSpecs)
            .build();

    // Creates a JavaFile in the destination package with the autoDelegator TypeSpec
    final var javaFile = JavaFile.builder(destinationPackage, autoDelegator).build();
    try {
      // Write the JavaFile to the local environment
      javaFile.writeTo(filer);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Problem writing " + destinationPackage + "." + className + " class to file", e);
    }
  }

  /**
   * @return a {@link Set} of {@link MethodSpec}s that delegate {@link #apisToDelegate} to an inner
   *     composed {@link #innerType}
   */
  private Set<MethodSpec> delegatingMethodSpecs() {
    return apisToDelegate.stream()
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
  }
}
