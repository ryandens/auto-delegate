plugins {
    id("com.diffplug.spotless") version "5.14.2"
    id("nebula.publish-verification") apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

allprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "nebula.publish-verification")
    apply(plugin = "nebula.contacts")
    apply(plugin = "nebula.info")
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
            googleJavaFormat("1.11.0")
        }
    }
    group = "com.ryandens"
    version = "0.2.2"

    this.extensions.getByType<nebula.plugin.contacts.ContactsExtension>().run {
        addPerson("admin@ryandens.com", delegateClosureOf<nebula.plugin.contacts.Contact> {
            moniker("Ryan Dens")
            role("owner")
            github("ryandens")
        })
    }
}
