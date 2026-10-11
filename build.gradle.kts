plugins {
    id("com.ryandens.delegation.base-conventions")
    alias(libs.plugins.nexus.publish)
}

nexusPublishing {
    packageGroup.set("com.ryandens")
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

// Keep daemon criteria reproducible without provisioning a missing CI JDK.
tasks.named<org.gradle.buildconfiguration.tasks.UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion.set(JavaLanguageVersion.of(27))
    toolchainDownloadUrls.empty()
}
