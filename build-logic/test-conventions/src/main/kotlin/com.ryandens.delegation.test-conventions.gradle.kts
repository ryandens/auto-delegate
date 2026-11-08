import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
}

// Accessor generation applies prerequisite plugins to a synthetic project without catalogs.
extensions.findByType<VersionCatalogsExtension>()?.find("libs")?.ifPresent { libs ->
    dependencies {
        testImplementation(platform(libs.findLibrary("junit-bom").orElseThrow()))
        testImplementation(libs.findLibrary("junit-jupiter").orElseThrow())
        testRuntimeOnly(libs.findLibrary("junit-platform-launcher").orElseThrow())
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
