plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.diffplug.spotless") version "5.10.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.netflix.nebula", "nebula-publishing-plugin", "17.3.2")
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
