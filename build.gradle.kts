plugins {
    id("com.ryandens.delegation.base-conventions")
    alias(libs.plugins.nexus.publish)
}

nexusPublishing {
    packageGroup.set("com.ryandens")
    repositories {
        sonatype {
            stagingProfileId.set("31cb749c34629")
        }
    }
}

// Keep daemon criteria reproducible without provisioning a missing CI JDK.
tasks.named<org.gradle.buildconfiguration.tasks.UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion.set(JavaLanguageVersion.of(27))
    toolchainDownloadUrls.empty()
}
