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

subprojects {
    spotless {
        // all subprojects must apply the java plugin
        java {
            googleJavaFormat("1.9")
        }
    }
    group = "com.ryandens.auto.delegate"
}
