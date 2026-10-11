plugins {
    id("com.diffplug.spotless")
    id("com.netflix.nebula.publish-verification")
    id("com.netflix.nebula.contacts")
    id("com.netflix.nebula.info")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
