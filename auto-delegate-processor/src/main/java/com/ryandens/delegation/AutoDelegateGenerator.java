package com.ryandens.delegation;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Uses {@link com.squareup.javapoet} to create a {@link JavaFile} that can be used to {@link
 * AutoDelegate} APIs
 */
final class AutoDelegateGenerator {
  private final String destinationPackage;
  private final String className;
  private final Map<DelegationTargetDescriptor, Set<ExecutableElement>> typeToExecutablesMap;
  private final List<DelegationTargetDescriptor> delegationTargetDescriptorList;
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
    this.typeUtils = typeUtils;
    // For each type we are auto-delegating to find all abstract ExecutableElements defined on
    // the interface and collect them into a Map, where the key is the DelegationTargetDescriptor
    // and the value is the Set<ExecutableElement> that must be delegated to by that
    // DelegationTargetDescriptor
    this.typeToExecutablesMap =
        delegationTargetDescriptorList.stream()
            .map(
                delegationTargetDescriptor ->
                    // create an entry mapping a DelegationTargetDescriptor to a
                    // Set<ExecutableElement>
                    new AbstractMap.SimpleEntry<>(
                        delegationTargetDescriptor,
                        elementUtils
                            // first get all the members for the delegation target
                            .getAllMembers(
                                (TypeElement) delegationTargetDescriptor.declaredType().asElement())
                            .stream()
                            // then, reduce it to only ExecutableElements
                            .filter(
                                typeElementMember -> typeElementMember instanceof ExecutableElement)
                            // then, map the members to the required type we reduced the stream to
                            .map(typeElementMember -> (ExecutableElement) typeElementMember)
                            // then, reduce it to the abstract APIs that we're interested in
                            // auto-delegating to
                            .filter(
                                typeElementMember ->
                                    typeElementMember.getModifiers().contains(Modifier.ABSTRACT)
                                        || typeElementMember
                                            .getModifiers()
                                            .contains(Modifier.DEFAULT))
                            // then, collect it into a Set<ExecutableElement> that the key
                            // DelegationTargetDescriptor should delegate to
                            .collect(Collectors.toSet())))
            // finally, collect the Map entries into a map (how does Collectors.toMap() not alias
            // Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)?!)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    for (Map.Entry<DelegationTargetDescriptor, Set<ExecutableElement>> entry :
        typeToExecutablesMap.entrySet()) {
      // generate the delegation methods to the abstract APIs we want auto-delegations for,
      // utilizing
      // the fields created above and assigned in the constructor
      final var methodSpecs = delegatingMethodSpecs(entry.getValue(), entry.getKey());
      // add those methods to the TypeSpec builder
      typeSpecBuilder.addMethods(methodSpecs);
    }

    // build the TypeSpec
    final var autoDelegator = typeSpecBuilder.build();

    // Creates a JavaFile in the destination package with the autoDelegator TypeSpec
    return JavaFile.builder(destinationPackage, autoDelegator).build();
  }

  /**
   * @return a {@link Set} of {@link MethodSpec}s that delegate to an inner composed implementation
   *     of the {@link javax.lang.model.type.DeclaredType} for the corresponding {@link
   *     ExecutableElement} identified by the provided {@link String} field name
   */
  private Set<MethodSpec> delegatingMethodSpecs(
      final Set<ExecutableElement> apisToDelegate, final DelegationTargetDescriptor descriptor) {
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
        .collect(Collectors.toSet());
  }
}
