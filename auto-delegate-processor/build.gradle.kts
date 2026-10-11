plugins {
    id("com.ryandens.delegation.java-conventions")
    `java-library`
    alias(libs.plugins.shadow)
    id("com.netflix.nebula.maven-shadow-publish")
    id("com.ryandens.delegation.publish")
}

description =
    """
    Annotation processor that generates auto-delegating abstract implementations of interfaces.
    """.trimIndent()

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        relocate("com.google.auto.common", "com.ryandens.delegation.shaded.auto.common")
    }
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.auto.common)
    implementation(project(":auto-delegate-annotations"))
    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service)
}
