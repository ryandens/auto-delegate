import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.ryandens.delegation.base-conventions")
    java
}

group = "com.ryandens"
version = "0.3.1"

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

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").orElseThrow()))
    testImplementation(libs.findLibrary("junit-jupiter").orElseThrow())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").orElseThrow())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
