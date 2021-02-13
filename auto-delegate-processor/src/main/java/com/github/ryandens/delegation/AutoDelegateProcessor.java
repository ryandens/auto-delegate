package com.github.ryandens.delegation;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;

/** TODO */
@AutoService(Processor.class)
public final class AutoDelegateProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private ProcessingEnvironment processingEnv;

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    this.processingEnv = processingEnv;
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
      // From the AnnotationMirror, get the value of AutoDelegate#apisToDelegate and translate it
      // into a Set<Element>
      final var apisToDelegate = getApisToDelegateFromAnnotationMirror(annotationMirror);
      // from the Element annotated with AutoDelegate, get their declared interfaces. Find all the
      // interfaces that are declared on the class that are also specified as delegation targets in
      // AutoDelegate#apisToDelegate
      final var type =
          (((TypeElement) element)
              .getInterfaces().stream()
                  .map(typeMirror -> (DeclaredType) typeMirror)
                  .filter(declaredType -> apisToDelegate.contains(declaredType.asElement()))
                  .findFirst() // TODO for now, only one interface to auto-delegate is supported
                  .get());

      // From the type we are auto-delegating to find all abstract ExecutableElements defined on the
      // interface and collect them into a set of ExecutableElements
      final var memberElements =
          processingEnv.getElementUtils().getAllMembers((TypeElement) type.asElement()).stream()
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

  /**
   * Returns the contents of a {@code Class[]}-typed "value" field in a given {@code
   * annotationMirror}.
   */
  private Set<Element> getApisToDelegateFromAnnotationMirror(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "apisToDelegate")
        .accept(
            new SimpleAnnotationValueVisitor8<Set<Element>, Void>() {
              @Override
              public Set<Element> visitType(TypeMirror typeMirror, Void v) {
                return Set.of(MoreTypes.asDeclared(typeMirror).asElement());
              }

              @Override
              public Set<Element> visitArray(List<? extends AnnotationValue> values, Void v) {
                return values.stream()
                    .flatMap(value -> value.accept(this, null).stream())
                    .collect(Collectors.toSet());
              }
            },
            null);
  }
}
