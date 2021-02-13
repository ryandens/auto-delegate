package com.github.ryandens.delegation;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
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
import static com.google.common.collect.ImmutableSet.toImmutableSet;

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
      final var annotationMirror =
          MoreElements.getAnnotationMirror(element, AutoDelegate.class)
              .toJavaUtil()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "element should always be annotated with AutoDelegate"));
      final var apisToDelegate = getValueFieldOfClasses(annotationMirror);
      //      final var elementsToDelegate =
      //          apisToDelegate.stream()
      //              .map(DeclaredType::asElement)
      //              .filter(declaredTypeElement -> declaredTypeElement instanceof TypeElement)
      //              .map(e -> (TypeElement) e)
      //              .collect(Collectors.toSet());

      final var type = ((DeclaredType) ((TypeElement) element).getInterfaces().get(0));
      final var memberElements =
          processingEnv.getElementUtils().getAllMembers((TypeElement) type.asElement()).stream()
              .filter(
                  typeElementMember -> typeElementMember.getModifiers().contains(Modifier.ABSTRACT))
              .filter(typeElementMember -> typeElementMember instanceof ExecutableElement)
              .map(typeElementMember -> (ExecutableElement) typeElementMember)
              .collect(Collectors.toSet());
      //      final var memberElements = new HashSet<ExecutableElement>();
      //      for (TypeElement typeElement : elementsToDelegate) {
      //        final List<? extends ExecutableElement> members =
      //            processingEnv.getElementUtils().getAllMembers(typeElement).stream()
      //                .filter(
      //                    typeElementMember ->
      //                        typeElementMember.getModifiers().contains(Modifier.ABSTRACT))
      //                .filter(typeElementMember -> typeElementMember instanceof ExecutableElement)
      //                .map(typeElementMember -> (ExecutableElement) typeElementMember)
      //                .collect(Collectors.toList());
      //        memberElements.addAll(members);
      //      }
      new AutoDelegateWriter(
              filer,
              "com.github.ryandens.examples",
              "AutoDelegate_InstrumentedSet",
              memberElements,
              type)
          .write();
    }
    return false;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(
      final Element element,
      final AnnotationMirror annotation,
      final ExecutableElement member,
      final String userText) {
    return null;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(AutoDelegate.class.getCanonicalName());
  }

  /**
   * Returns the contents of a {@code Class[]}-typed "value" field in a given {@code
   * annotationMirror}.
   */
  private ImmutableSet<DeclaredType> getValueFieldOfClasses(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "apisToDelegate")
        .accept(
            new SimpleAnnotationValueVisitor8<ImmutableSet<DeclaredType>, Void>() {
              @Override
              public ImmutableSet<DeclaredType> visitType(TypeMirror typeMirror, Void v) {
                // TODO(ronshapiro): class literals may not always be declared types, i.e.
                // int.class, int[].class
                return ImmutableSet.of(MoreTypes.asDeclared(typeMirror));
              }

              @Override
              public ImmutableSet<DeclaredType> visitArray(
                  List<? extends AnnotationValue> values, Void v) {
                return values.stream()
                    .flatMap(value -> value.accept(this, null).stream())
                    .collect(toImmutableSet());
              }
            },
            null);
  }
}
