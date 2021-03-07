package com.ryandens.delegation;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

/**
 * Annotation processor that generates abstract classes that delegate to an inner composed
 * implementation of an interface.
 */
@AutoService(Processor.class)
public final class AutoDelegateProcessor extends AbstractProcessor {

  private Filer filer;
  private Elements elementUtils;

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    filer = processingEnv.getFiler();
    elementUtils = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> autoDelegateElements =
        roundEnv.getElementsAnnotatedWith(AutoDelegate.class);
    //
    for (final Element element : autoDelegateElements) {
      // First, get an AnnotationMirror off the element annotated with AutoDelegate. This can be
      // thought of as an "instance" of an annotation
      final var annotationMirror =
          MoreElements.getAnnotationMirror(element, AutoDelegate.class)
              .toJavaUtil()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "element should always be annotated with AutoDelegate"));
      // From the AnnotationMirror, get the value of AutoDelegate#value and translate it
      // into an Element
      final var valueToDelegateField = getInterfaceToDelegateAsElement(annotationMirror);
      final var toDelegateSetField = getInterfacesToDelegateAsElementSet(annotationMirror);

      if (valueToDelegateField == null && toDelegateSetField.isEmpty()) {
        throw new IllegalArgumentException("A delegation target must be provided");
      } else if (valueToDelegateField != null && !toDelegateSetField.isEmpty()) {
        throw new IllegalArgumentException(
            "Only one mechanism of supplying delegation targets should be used");
      }
      final var apisToDelegate =
          valueToDelegateField != null ? List.of(valueToDelegateField) : toDelegateSetField;
      // from the Element annotated with AutoDelegate, get their declared interfaces. Find the
      // interface that is also specified as the delegation target in AutoDelegate#value

      final var types =
          (((TypeElement) element)
              .getInterfaces().stream()
                  .map(typeMirror -> (DeclaredType) typeMirror)
                  .filter(declaredType -> apisToDelegate.contains(declaredType.asElement()))
                  .collect(Collectors.toList()));

      final var mutableDelegationTargetDescriptors = new LinkedList<DelegationTargetDescriptor>();
      for (int i = 0; i < types.size(); i++) {
        mutableDelegationTargetDescriptors.add(
            new DelegationTargetDescriptor(types.get(i), "inner" + i));
      }

      if (types.size() == 0) {
        throw new IllegalArgumentException(
            "No interfaces declared on the class match the element specified in AutoDelegate#value");
      }

      final var delegationTargetDescriptors =
          Collections.unmodifiableList(mutableDelegationTargetDescriptors);

      // From each type we are auto-delegating to find all abstract ExecutableElements defined on
      // the
      // interface and collect them into a set of ExecutableElements
      final var memberElementsMap =
          delegationTargetDescriptors.stream()
              .map(
                  delegationTargetDescriptor ->
                      new AbstractMap.SimpleEntry<>(
                          delegationTargetDescriptor,
                          elementUtils
                              .getAllMembers(
                                  (TypeElement)
                                      delegationTargetDescriptor.declaredType().asElement())
                              .stream()
                              .filter(
                                  typeElementMember ->
                                      typeElementMember.getModifiers().contains(Modifier.ABSTRACT))
                              .filter(
                                  typeElementMember ->
                                      typeElementMember instanceof ExecutableElement)
                              .map(typeElementMember -> (ExecutableElement) typeElementMember)
                              .collect(Collectors.toSet())))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      // Get the package of the element annotated with AutoDelegate, as the
      final var destinationPackageName =
          MoreElements.getPackage(element).getQualifiedName().toString();
      new AutoDelegateWriter(
              filer,
              destinationPackageName,
              "AutoDelegate_" + element.getSimpleName(),
              delegationTargetDescriptors,
              memberElementsMap)
          .write();
    }
    return false;
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(AutoDelegate.class.getCanonicalName());
  }

  /** Returns the contents of {@link AutoDelegate#value()} as an {@link Element} */
  private Element getInterfaceToDelegateAsElement(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "value")
        .accept(
            new SimpleAnnotationValueVisitor8<Element, Void>() {
              @Override
              public Element visitType(TypeMirror typeMirror, Void v) {
                if (typeMirror.getKind().equals(TypeKind.VOID)) {
                  return null;
                } else {
                  return MoreTypes.asDeclared(typeMirror).asElement();
                }
              }
            },
            null);
  }

  /** Returns the contents of {@link AutoDelegate#value()} as an {@link Element} */
  private List<Element> getInterfacesToDelegateAsElementSet(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "to")
        .accept(
            new SimpleAnnotationValueVisitor8<List<Element>, Void>() {
              @Override
              public List<Element> visitType(TypeMirror typeMirror, Void v) {
                if (typeMirror.getKind().equals(TypeKind.VOID)) {
                  return null;
                } else {
                  return List.of(MoreTypes.asDeclared(typeMirror).asElement());
                }
              }

              @Override
              public List<Element> visitArray(
                  final List<? extends AnnotationValue> values, final Void unused) {
                return values.stream()
                    .flatMap(value -> value.accept(this, null).stream())
                    .collect(Collectors.toList());
              }
            },
            null);
  }
}
