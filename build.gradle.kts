plugins {
    id("com.diffplug.spotless") version "8.0.0"
    id("com.netflix.nebula.publish-verification") apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

allprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.netflix.nebula.publish-verification")
    apply(plugin = "com.netflix.nebula.contacts")
    apply(plugin = "com.netflix.nebula.info")
    spotless {
        kotlinGradle {
            ktlint()
        }
    }
}

nexusPublishing {
    packageGroup.set("com.ryandens")
    repositories {
        sonatype {
            stagingProfileId.set("31cb749c34629")
        }
    }
}

subprojects {
    spotless {
        // all subprojects must apply the java plugin
        java {
            googleJavaFormat()
        }
    }
    group = "com.ryandens"
    version = "0.3.1"

    this.extensions.getByType<nebula.plugin.contacts.ContactsExtension>().run {
        addPerson(
            "admin@ryandens.com",
            delegateClosureOf<nebula.plugin.contacts.Contact> {
                moniker("Ryan Dens")
                role("owner")
                github("ryandens")
            },
        )
    }
}
