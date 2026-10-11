plugins {
    `kotlin-dsl`
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
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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

tasks.test {
    useJUnitPlatform()
}
