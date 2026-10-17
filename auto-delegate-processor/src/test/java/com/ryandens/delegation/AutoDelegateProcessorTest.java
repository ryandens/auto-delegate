package com.ryandens.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AutoDelegateProcessorTest {
  @TempDir Path directory;

  @Test
  void failsProcessorForClassTargetsInBothAnnotationForms() throws Exception {
    for (final var annotation :
        List.of(
            "@AutoDelegate(FileSystemProvider.class)",
            "@AutoDelegate(to = {Runnable.class, FileSystemProvider.class})")) {
      final var source = directory.resolve("Example.java");
      Files.writeString(
          source,
          """
          package example;

          import com.ryandens.delegation.AutoDelegate;
          import java.nio.file.spi.FileSystemProvider;

          %s
          abstract class Example implements Runnable {}
          """
              .formatted(annotation));
      final var compiler = ToolProvider.getSystemJavaCompiler();
      final var diagnostics = new StringWriter();
      try (final var fileManager = compiler.getStandardFileManager(null, null, null)) {
        final var task =
            compiler.getTask(
                diagnostics,
                fileManager,
                null,
                List.of("--release", "11", "-d", directory.toString(), "-s", directory.toString()),
                null,
                fileManager.getJavaFileObjects(source.toFile()));
        task.setProcessors(List.of(new AutoDelegateProcessor()));
        final var failure = assertThrows(RuntimeException.class, task::call);
        final var cause = assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertTrue(
            cause
                .getMessage()
                .contains(
                    "@AutoDelegate only supports interface targets; java.nio.file.spi.FileSystemProvider"),
            cause.getMessage());
        assertTrue(cause.getMessage().contains("Write delegation manually"), cause.getMessage());
      }
      assertFalse(Files.exists(directory.resolve("example/AutoDelegate_Example.java")));
    }
  }

  @Test
  void generatesMethodsInInterfaceAndMethodDeclarationOrder() throws Exception {
    final var source = directory.resolve("Example.java");
    Files.writeString(
        source,
        """
        package example;

        import com.ryandens.delegation.AutoDelegate;

        interface First {
          void zebra();
          default int middle() { return 1; }
          void alpha();
          void alpha(int value);
          static void ignored() {}
        }

        interface Left {
          void shared();
        }

        interface Right {
          void shared();
        }

        interface Second extends Left, Right {
          void yellow();
          void beta();
        }

        @AutoDelegate(to = {Second.class, First.class})
        abstract class Example implements First, Second {}
        """);
    final var compiler = ToolProvider.getSystemJavaCompiler();
    final var diagnostics = new StringWriter();
    try (final var fileManager = compiler.getStandardFileManager(null, null, null)) {
      final var task =
          compiler.getTask(
              diagnostics,
              fileManager,
              null,
              List.of("--release", "11", "-d", directory.toString(), "-s", directory.toString()),
              null,
              fileManager.getJavaFileObjects(source.toFile()));
      task.setProcessors(List.of(new AutoDelegateProcessor()));
      assertTrue(task.call(), diagnostics.toString());
    }

    final var generated = Files.readString(directory.resolve("example/AutoDelegate_Example.java"));
    final var methods =
        Pattern.compile("public (?:void|int) (\\w+\\([^)]*\\))")
            .matcher(generated)
            .results()
            .map(match -> match.group(1))
            .toList();
    assertEquals(
        List.of(
            "zebra()", "middle()", "alpha()", "alpha(int value)", "shared()", "yellow()", "beta()"),
        methods,
        generated);
  }
}
