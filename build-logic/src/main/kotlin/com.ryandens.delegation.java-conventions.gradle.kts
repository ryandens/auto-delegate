import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension

plugins {
    id("com.ryandens.delegation.base-conventions")
    id("com.ryandens.delegation.test-conventions")
}

spotless {
    java {
        googleJavaFormat()
    }
}

extensions.configure<ContactsExtension> {
    addPerson(
        "admin@ryandens.com",
        delegateClosureOf<Contact> {
            moniker("Ryan Dens")
            role("owner")
            github("ryandens")
        },
    )
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(27))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Production artifacts target Java 11; tests use the JDK 27 toolchain defaults.
tasks.compileJava {
    options.release.set(11)
}
