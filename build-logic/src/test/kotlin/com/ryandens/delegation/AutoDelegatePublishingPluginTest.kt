package com.ryandens.delegation

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

class AutoDelegatePublishingPluginTest {
    @TempDir
    lateinit var projectDir: Path

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `publishing conventions produce artifacts and reuse configuration cache`(publishFirst: Boolean) {
        val plugins = listOf("id(\"com.ryandens.delegation.publish\")", "`java-library`")
        val pluginDeclarations = (if (publishFirst) plugins else plugins.reversed()).joinToString("\n")
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                $pluginDeclarations
            }
            group = "example"
            version = "1.0"
            description = "Publishing convention fixture"
            java {
                toolchain.languageVersion.set(JavaLanguageVersion.of(27))
            }
            """.trimIndent(),
        )
        val source = projectDir.resolve("src/main/java/example/Example.java")
        source.parent.createDirectories()
        source.writeText("package example; /** Example API. */ public class Example {}")

        val runner =
            GradleRunner
                .create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(
                    "jar",
                    "sourcesJar",
                    "javadocJar",
                    "generatePomFileForNebulaPublication",
                    "--configuration-cache",
                    "--build-cache",
                    "--warning-mode=fail",
                    "--stacktrace",
                )
        val first = runner.build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":jar")?.outcome)
        val pom = projectDir.resolve("build/publications/nebula/pom-default.xml")
        val firstPom = pom.readText()
        assertTrue(firstPom.contains("<artifactId>fixture</artifactId>"))
        assertTrue(firstPom.contains("<name>The Apache Software License, Version 2.0</name>"))
        for ((suffix, entry) in listOf(
            "" to "example/Example.class",
            "-sources" to "example/Example.java",
            "-javadoc" to "example/Example.html",
        )) {
            JarFile(projectDir.resolve("build/libs/fixture-1.0$suffix.jar").toFile()).use {
                assertTrue(it.getJarEntry(entry) != null, "Missing $entry in $suffix jar")
            }
        }

        val second = runner.build()
        assertTrue(second.output.contains("Reusing configuration cache."))
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":jar")?.outcome)
        assertEquals(firstPom, pom.readText())

        val rebuilt = runner.withArguments(listOf("clean") + runner.arguments).build()
        assertEquals(TaskOutcome.FROM_CACHE, rebuilt.task(":compileJava")?.outcome)
        assertEquals(firstPom, pom.readText())
    }
}
