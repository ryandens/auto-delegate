package com.ryandens.delegation

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date
import java.util.jar.JarFile
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText

class AutoDelegatePublishingPluginTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `signing works with in-memory credentials and configuration cache`() {
        Security.addProvider(BouncyCastleProvider())
        val password = "temporary-test-password"
        val keyPair =
            JcaPGPKeyPair(
                PublicKeyPacket.VERSION_4,
                PGPPublicKey.RSA_GENERAL,
                KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair(),
                Date(),
            )
        val digest = JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build()
        val secretKey =
            PGPSecretKey(
                keyPair.privateKey,
                keyPair.publicKey,
                digest.get(HashAlgorithmTags.SHA1),
                true,
                JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, digest.get(HashAlgorithmTags.SHA256))
                    .setProvider("BC")
                    .build(password.toCharArray()),
            )
        val key =
            ByteArrayOutputStream()
                .also { output ->
                    ArmoredOutputStream(output).use { secretKey.encode(it) }
                }.toString(Charsets.UTF_8)
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("com.ryandens.delegation.publish")
            }
            group = "example"
            version = "1.0"
            description = "Signing fixture"
            java { toolchain.languageVersion.set(JavaLanguageVersion.of(27)) }
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
                .withEnvironment(
                    System.getenv() +
                        mapOf(
                            "ORG_GRADLE_PROJECT_signingKey" to key,
                            "ORG_GRADLE_PROJECT_signingPassword" to password,
                        ),
                ).withArguments("signNebulaPublication", "--configuration-cache", "--warning-mode=fail", "--stacktrace")
        assertEquals(TaskOutcome.SUCCESS, runner.build().task(":signNebulaPublication")?.outcome)
        val second = runner.withArguments(runner.arguments + "--rerun-tasks").build()
        assertTrue(second.output.contains("Reusing configuration cache."))
        assertEquals(TaskOutcome.SUCCESS, second.task(":signNebulaPublication")?.outcome)
        val artifact = projectDir.resolve("build/libs/fixture-1.0.jar")
        val signature =
            projectDir.resolve("build/libs/fixture-1.0.jar.asc").inputStream().use { input ->
                PGPUtil.getDecoderStream(input).use { decoded ->
                    (PGPObjectFactory(decoded, JcaKeyFingerprintCalculator()).nextObject() as PGPSignatureList)[0]
                }
            }
        signature.init(JcaPGPContentVerifierBuilderProvider().setProvider("BC"), secretKey.publicKey)
        signature.update(artifact.readBytes())
        assertTrue(signature.verify(), "Signature must verify against the temporary public key")

        val missingKey =
            runner
                .withEnvironment(
                    System.getenv().filterKeys { it !in setOf("ORG_GRADLE_PROJECT_signingKey", "ORG_GRADLE_PROJECT_signingPassword") },
                ).buildAndFail()
        assertTrue(missingKey.output.contains("no configured signatory", ignoreCase = true))
    }

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
