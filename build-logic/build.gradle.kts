plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.10.4"
}

// Build plugins run on Gradle's Java 17+ API, independently of library bytecode.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(27))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.netflix.nebula.maven-nebula-publish:com.netflix.nebula.maven-nebula-publish.gradle.plugin:23.1.0")
    // Fixes the Gradle 10 findAll deprecation in the publishing plugin's transitive version.
    implementation("com.netflix.nebula.info:com.netflix.nebula.info.gradle.plugin:16.3.0")
}

gradlePlugin {
    plugins {
        register("auto-delegate-publish") {
            id = "com.ryandens.delegation.publish"
            implementationClass = "com.ryandens.delegation.AutoDelegatePublishingPlugin"
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/**/*.gradle.kts")
        ktlint()
    }
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}
