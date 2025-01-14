plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.2"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.netflix.nebula.maven-nebula-publish", "com.netflix.nebula.maven-nebula-publish.gradle.plugin", "21.1.0")
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
        ktlint()
    }
    kotlin {
        ktlint()
    }
}
