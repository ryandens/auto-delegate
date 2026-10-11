plugins {
    id("com.ryandens.delegation.base-conventions")
    alias(libs.plugins.nexus.publish)
}

nexusPublishing {
    packageGroup.set("com.ryandens")
    repositories {
        sonatype {
            // Let the plugin discover the profile for com.ryandens on the new service;
            // the previous numeric stagingProfileId belonged to the retired OSSRH service.
            // Sonatype's Central publishing endpoints for the Nexus plugin:
            // https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuring-your-plugin
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
