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
    // create a MethodSpec for the constructor
    final var constructorBuilder = MethodSpec.constructorBuilder();
    for (DelegationTargetDescriptor descriptor : delegationTargetDescriptorList) {
      // add type variables required to implement this type
      final var typeVariables =
          MoreTypes.asTypeElement(descriptor.declaredType()).getTypeParameters().stream()
              .map(TypeVariableName::get)
              .collect(Collectors.toSet());
      typeSpecBuilder.addTypeVariables(typeVariables);

      //  implement the specified interface for this delegation target
      typeSpecBuilder.addSuperinterface(descriptor.declaredType());

      // add a field for the specified descriptor
      final var innerField =
          FieldSpec.builder(
                  TypeName.get(descriptor.declaredType()),
                  descriptor.fieldName(),
                  Modifier.FINAL,
                  Modifier.PRIVATE)
              .build();
      typeSpecBuilder.addField(innerField);

      // modify the constructor MethodSpec to take an instance of the specified declaredType and
      // assigns it to a field with name matching the field we just created
      constructorBuilder
          .addParameter(
              TypeName.get(descriptor.declaredType()), descriptor.fieldName(), Modifier.FINAL)
          .addCode(
              CodeBlock.builder()
                  .add("this." + descriptor.fieldName() + "= " + descriptor.fieldName() + ";")
                  .build());
    }
    // build the constructor and add it to the MethodSpec
    typeSpecBuilder.addMethod(constructorBuilder.build());

    for (Map.Entry<DelegationTargetDescriptor, Set<ExecutableElement>> entry :
        typeToExecutablesMap.entrySet()) {
      // generate the delegation methods to the abstract APIs we want auto-delegations for,
      // utilizing
      // the fields created above and assigned in the constructor
      final var methodSpecs = delegatingMethodSpecs(entry.getValue(), entry.getKey().fieldName());
      // add those methods to the TypeSpec builder
      typeSpecBuilder.addMethods(methodSpecs);
    }

    // build the TypeSpec
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
   * @return a {@link Set} of {@link MethodSpec}s that delegate to an inner composed implementation
   *     of the {@link javax.lang.model.type.DeclaredType} for the corresponding {@link
   *     ExecutableElement} identified by the provided {@link String} field name
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
