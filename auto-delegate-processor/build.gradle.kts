plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version ("6.1.0")
    id("nebula.maven-publish") version ("17.3.2")
    id("nebula.maven-shadow-publish") version ("17.3.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

tasks.jar {
    enabled = false
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
