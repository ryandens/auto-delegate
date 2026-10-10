package com.ryandens.delegation;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Uses {@link com.squareup.javapoet} to create a {@link JavaFile} that can be used to {@link
 * AutoDelegate} APIs
 */
final class AutoDelegateGenerator {
  private final String destinationPackage;
  private final String className;
  private final List<DelegationTargetDescriptor> delegationTargetDescriptorList;
  private final Elements elementUtils;
  private final Types typeUtils;

  /**
   * @param destinationPackage where the Java class should be written to
   * @param className of the generated Java class
   * @param delegationTargetDescriptorList a {@link List} of {@link DelegationTargetDescriptor}s
   *     that this class should delegate to
   */
  AutoDelegateGenerator(
      final Elements elementUtils,
      final Types typeUtils,
      final String destinationPackage,
      final String className,
      final List<DelegationTargetDescriptor> delegationTargetDescriptorList) {
    this.destinationPackage = Objects.requireNonNull(destinationPackage);
    this.className = Objects.requireNonNull(className);
    this.delegationTargetDescriptorList = Objects.requireNonNull(delegationTargetDescriptorList);
    this.elementUtils = elementUtils;
    this.typeUtils = typeUtils;
  }

  JavaFile autoDelegate() {
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
              .collect(Collectors.toUnmodifiableList());
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

    for (DelegationTargetDescriptor descriptor : delegationTargetDescriptorList) {
      // generate the delegation methods to the abstract APIs we want auto-delegations for,
      // utilizing the fields created above and assigned in the constructor
      final var methodSpecs =
          delegatingMethodSpecs(
              apisToDelegate(MoreTypes.asTypeElement(descriptor.declaredType())), descriptor);
      // add those methods to the TypeSpec builder
      typeSpecBuilder.addMethods(methodSpecs);
    }

    // build the TypeSpec
    final var autoDelegator = typeSpecBuilder.build();

    // Creates a JavaFile in the destination package with the autoDelegator TypeSpec
    return JavaFile.builder(destinationPackage, autoDelegator).build();
  }

  /**
   * Finds all abstract and default methods of the provided delegation target, including those
   * inherited from superinterfaces. The methods are returned in declaration order, with methods
   * declared on a type before those of its superinterfaces, so that the generated source is the
   * same on each build.
   *
   * @return a {@link List} of {@link ExecutableElement}s that must be delegated to
   */
  private List<ExecutableElement> apisToDelegate(final TypeElement delegationTarget) {
    final var declarationOrder = new HashMap<ExecutableElement, Integer>();
    indexInDeclarationOrder(delegationTarget, declarationOrder);
    return ElementFilter.methodsIn(elementUtils.getAllMembers(delegationTarget)).stream()
        .filter(
            method ->
                method.getModifiers().contains(Modifier.ABSTRACT)
                    || method.getModifiers().contains(Modifier.DEFAULT))
        .sorted(
            Comparator.comparingInt(
                method -> declarationOrder.getOrDefault(method, Integer.MAX_VALUE)))
        .collect(Collectors.toList());
  }

  /**
   * Assigns an index to each method declared on the provided type and then on its superinterfaces,
   * depth first in declaration order. A method reachable by more than one path keeps its first
   * index.
   */
  private static void indexInDeclarationOrder(
      final TypeElement type, final Map<ExecutableElement, Integer> declarationOrder) {
    for (final ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      declarationOrder.putIfAbsent(method, declarationOrder.size());
    }
    for (final TypeMirror superinterface : type.getInterfaces()) {
      indexInDeclarationOrder(MoreTypes.asTypeElement(superinterface), declarationOrder);
    }
  }

  /**
   * @return a {@link List} of {@link MethodSpec}s, in the order of the provided {@link
   *     ExecutableElement}s, that delegate to an inner composed implementation of the {@link
   *     javax.lang.model.type.DeclaredType} for the corresponding {@link ExecutableElement}
   *     identified by the provided {@link String} field name
   */
  private List<MethodSpec> delegatingMethodSpecs(
      final List<ExecutableElement> apisToDelegate, final DelegationTargetDescriptor descriptor) {
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

              return MethodSpec.overriding(executableElement, descriptor.declaredType(), typeUtils)
                  .addCode(
                      CodeBlock.builder()
                          .addStatement(
                              returnPrefix
                                  + descriptor.fieldName()
                                  + "."
                                  + executableElement.getSimpleName().toString()
                                  + "("
                                  + parameters
                                  + ")")
                          .build())
                  .build();
            })
        // a method inherited from more than one superinterface is generated identically for each
        .distinct()
        .collect(Collectors.toList());
  }
}
