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
    apply(plugin = "nebula.contacts")
    apply(plugin = "nebula.info")
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

    this.extensions.getByType<nebula.plugin.contacts.ContactsExtension>().run {
        addPerson("admin@ryandens.com", delegateClosureOf<nebula.plugin.contacts.Contact> {
            moniker("Ryan Dens")
            role("owner")
            github("ryandens")
        })
    }
}
