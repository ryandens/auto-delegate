plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version ("6.1.0")
    id("nebula.maven-shadow-publish")
    id("com.ryandens.delegation.publish")
}

description = """
    Annotation processor that generates auto-delegating abstract implementations of interfaces.
""".trimIndent()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        relocate("com.google.auto.common", "com.ryandens.delegation.shaded.auto.common")
    }
}

dependencies {
    implementation("com.squareup", "javapoet", "1.13.0")
    implementation("com.google.auto", "auto-common", "0.11")
    implementation(project(":auto-delegate-annotations"))
    val autoServiceVersion = "1.0-rc7"
    compileOnly("com.google.auto.service", "auto-service-annotations", autoServiceVersion)
    annotationProcessor("com.google.auto.service", "auto-service", autoServiceVersion)
}
