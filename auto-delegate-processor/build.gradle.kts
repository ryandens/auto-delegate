plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.netflix.nebula.maven-shadow-publish")
    id("com.ryandens.delegation.publish")
}

description =
    """
    Annotation processor that generates auto-delegating abstract implementations of interfaces.
    """.trimIndent()

tasks.compileJava {
    options.release.set(11)
}

tasks.compileTestJava {
    options.release.set(17)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        relocate("com.google.auto.common", "com.ryandens.delegation.shaded.auto.common")
    }
}

dependencies {
    implementation("com.squareup", "javapoet", "1.13.0")
    implementation("com.google.auto", "auto-common", "1.1.2")
    implementation(project(":auto-delegate-annotations"))
    val autoServiceVersion = "1.0"
    compileOnly("com.google.auto.service", "auto-service-annotations", autoServiceVersion)
    annotationProcessor("com.google.auto.service", "auto-service", autoServiceVersion)
}
