plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.1.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.netflix.nebula.maven-nebula-publish", "com.netflix.nebula.maven-nebula-publish.gradle.plugin", "22.2.0")
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
