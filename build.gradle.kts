plugins {
    id("com.diffplug.spotless") version "5.10.1"
    id("nebula.publish-verification") version "17.3.2" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "nebula.publish-verification")
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
    version = "0.1.0"
}
