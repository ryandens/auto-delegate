package com.ryandens.delegation;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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
      final var apisToDelegate = getInterfaceToDelegateAsElement(annotationMirror);
      // from the Element annotated with AutoDelegate, get their declared interfaces. Find the
      // interface that is also specified as the delegation target in AutoDelegate#value
      final var type =
          (((TypeElement) element)
              .getInterfaces().stream()
                  .map(typeMirror -> (DeclaredType) typeMirror)
                  .filter(declaredType -> apisToDelegate.equals(declaredType.asElement()))
                  .collect(
                      Collectors.collectingAndThen(
                          Collectors.toList(),
                          list -> {
                            if (list.size() == 0) {
                              throw new IllegalArgumentException(
                                  "No interfaces declared on the class match the element specified in AutoDelegate#value");
                            } else if (list.size() > 1) {
                              throw new IllegalStateException(
                                  "Multiple interfaces declared on the class match the element specified in AutoDelegate#value");
                            } else {
                              return list.get(0);
                            }
                          })));

      // From the type we are auto-delegating to find all abstract ExecutableElements defined on the
      // interface and collect them into a set of ExecutableElements
      final var memberElements =
          elementUtils.getAllMembers((TypeElement) type.asElement()).stream()
              .filter(
                  typeElementMember -> typeElementMember.getModifiers().contains(Modifier.ABSTRACT))
              .filter(typeElementMember -> typeElementMember instanceof ExecutableElement)
              .map(typeElementMember -> (ExecutableElement) typeElementMember)
              .collect(Collectors.toSet());
      // Get the package of the element annotated with AutoDelegate, as the
      final var destinationPackageName =
          MoreElements.getPackage(element).getQualifiedName().toString();
      new AutoDelegateWriter(
              filer,
              destinationPackageName,
              "AutoDelegate_" + element.getSimpleName(),
              memberElements,
              type)
          .write();
    }
    return false;
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
                return MoreTypes.asDeclared(typeMirror).asElement();
              }
            },
            null);
  }
}
