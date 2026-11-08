plugins {
    `kotlin-dsl`
    id("com.ryandens.delegation.test-conventions")
    alias(libs.plugins.spotless)
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

dependencies {
    implementation(libs.nebula.publish.plugin)
    // Fixes the Gradle 10 findAll deprecation in the publishing plugin's transitive version.
    implementation(libs.nebula.info.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.nebula.contacts.plugin)
    implementation("com.ryandens.delegation:test-conventions")
    testImplementation(gradleTestKit())
    // Generate and verify ephemeral signing keys with the PGP implementation shipped by Gradle.
    testImplementation(
        fileTree(gradle.gradleHomeDir!!.resolve("lib/plugins")) {
            include("bcpg-*.jar", "bcprov-*.jar", "bcutil-*.jar")
        },
    )
}

gradlePlugin {
    // Registers the existing binary publishing plugin so the included build can resolve its ID.
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
