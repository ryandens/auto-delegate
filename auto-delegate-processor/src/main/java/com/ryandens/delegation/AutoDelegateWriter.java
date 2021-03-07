package com.ryandens.delegation;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Uses {@link com.squareup.javapoet} to write the class file that can be used to {@link
 * AutoDelegate} APIs
 */
final class AutoDelegateWriter {
  private final Filer filer;
  private final String destinationPackage;
  private final String className;
  private final Map<DelegationTargetDescriptor, Set<ExecutableElement>> typeToExecutablesMap;
  private final List<DelegationTargetDescriptor> delegationTargetDescriptorList;

  /**
   * @param filer for writing generated source code to the local environment
   * @param destinationPackage where the Java class should be written to
   * @param className of the generated Java class
   */
  AutoDelegateWriter(
      final Filer filer,
      final String destinationPackage,
      final String className,
      final List<DelegationTargetDescriptor> delegationTargetDescriptorList,
      final Map<DelegationTargetDescriptor, Set<ExecutableElement>> typeToExecutablesMap) {
    this.filer = filer;
    this.destinationPackage = destinationPackage;
    this.className = className;
    this.delegationTargetDescriptorList = delegationTargetDescriptorList;
    this.typeToExecutablesMap = typeToExecutablesMap;
  }

  void write() {
    final var typeSpecBuilder =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.ABSTRACT)
            .addJavadoc(
                "Shallowly immutable, shallowly thread-safe abstract class that forwards to an inner composed types");
    final var constructorBuilder = MethodSpec.constructorBuilder();
    for (DelegationTargetDescriptor descriptor : delegationTargetDescriptorList) {
      // creates a MethodSpec for the constructor that takes an instance of type innerType and
      // assigns it to a field with name "inner"
      constructorBuilder
          .addParameter(
              TypeName.get(descriptor.declaredType()), descriptor.fieldName(), Modifier.FINAL)
          .addCode(
              CodeBlock.builder()
                  .add("this." + descriptor.fieldName() + "= " + descriptor.fieldName() + ";")
                  .build());
    }
    typeSpecBuilder.addMethod(constructorBuilder.build());
    for (Map.Entry<DelegationTargetDescriptor, Set<ExecutableElement>> entry :
        typeToExecutablesMap.entrySet()) {
      write(
          entry.getKey().declaredType(),
          entry.getValue(),
          typeSpecBuilder,
          entry.getKey().fieldName());
    }

    final var autoDelegator = typeSpecBuilder.build();

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
   * Builds the {@link TypeSpec} for the APIs specified by {@link AutoDelegate#apisToDelegate()} and
   * writes the {@link JavaFile} to the provided {@link Filer}
   */
  private void write(
      final TypeMirror innerType,
      final Set<ExecutableElement> apisToDelegate,
      final TypeSpec.Builder typeSpecBuilder,
      final String fieldName) {
    final var innerField =
        FieldSpec.builder(TypeName.get(innerType), fieldName, Modifier.FINAL, Modifier.PRIVATE)
            .build();
    // Get the TypeVariables from the innerType
    final var typeVariables =
        MoreTypes.asTypeElement(innerType).getTypeParameters().stream()
            .map(TypeVariableName::get)
            .collect(Collectors.toSet());

    // create MethodSpecs for the apisToDelegate
    final Set<MethodSpec> methodSpecs = delegatingMethodSpecs(apisToDelegate, fieldName);

    typeSpecBuilder
        .addSuperinterface(innerType)
        .addTypeVariables(typeVariables)
        .addField(innerField)
        .addMethods(methodSpecs);
  }

  /**
   * @return a {@link Set} of {@link MethodSpec}s that delegate {@link #apisToDelegate} to an inner
   *     composed {@link #innerType}
   */
  private Set<MethodSpec> delegatingMethodSpecs(
      final Set<ExecutableElement> apisToDelegate, final String fieldName) {
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
                                  + fieldName
                                  + "."
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
