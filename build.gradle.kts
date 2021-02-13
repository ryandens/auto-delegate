plugins {
    id("com.diffplug.spotless") version "5.10.1"
}

allprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "com.diffplug.spotless")
    spotless {
        kotlinGradle {
            ktlint()
        }
    }
}
