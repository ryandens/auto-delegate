package com.github.ryandens.delegation;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** TODO */
@SupportedSourceVersion(SourceVersion.RELEASE_15)
@AutoService(Processor.class)
public final class AutoDelegateProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> autoDelegateElements =
        roundEnv.getElementsAnnotatedWith(AutoDelegate.class);
    for (final Element element : autoDelegateElements) {
      autoDelegateElements.stream().findFirst().get().getEnclosingElement().asType().toString();
      final var enclosingElement = element.getEnclosingElement();
      if (!ElementKind.PACKAGE.equals(enclosingElement.getKind())) {
        throw new IllegalStateException("ElementKind of the annotated type is not a package");
      }
      final var packageName = enclosingElement.asType().toString();
      final var className = "AutoDelegate_" + element.getSimpleName().toString();
      final var autoDelegateAnnotation = element.getAnnotation(AutoDelegate.class);
      final var declaringType = element.asType();
      List<Class<?>> apisToDelegate = null;
      try {
        autoDelegateAnnotation.apisToDelegate();
      } catch (MirroredTypesException e) {
        apisToDelegate =
            e.getTypeMirrors().stream()
                .map(typeMirror -> typeMirror.getKind().getDeclaringClass())
                .collect(Collectors.toList());
      }
      final var delegatingFieldName = autoDelegateAnnotation.value();
      new AutoDelegateWriter(
              filer, packageName, className, delegatingFieldName, apisToDelegate, declaringType)
          .write();
    }
    return true;
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
}
