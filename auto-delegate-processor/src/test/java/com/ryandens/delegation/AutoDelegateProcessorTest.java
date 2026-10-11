package com.ryandens.delegation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AutoDelegateProcessorTest {
  @TempDir Path directory;

  @Test
  void generatesMethodsInInterfaceAndMethodDeclarationOrder() throws Exception {
    final var source = directory.resolve("Example.java");
    Files.writeString(
        source,
        String.join(
            "\n",
            "package example;",
            "",
            "import com.ryandens.delegation.AutoDelegate;",
            "",
            "interface First {",
            "  void zebra();",
            "  default int middle() { return 1; }",
            "  void alpha();",
            "  void alpha(int value);",
            "  static void ignored() {}",
            "}",
            "",
            "interface Left {",
            "  void shared();",
            "}",
            "",
            "interface Right {",
            "  void shared();",
            "}",
            "",
            "interface Second extends Left, Right {",
            "  void yellow();",
            "  void beta();",
            "}",
            "",
            "@AutoDelegate(to = {Second.class, First.class})",
            "abstract class Example implements First, Second {}"));
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
            .collect(Collectors.toList());
    assertEquals(
        List.of(
            "zebra()", "middle()", "alpha()", "alpha(int value)", "shared()", "yellow()", "beta()"),
        methods,
        generated);
  }
}
