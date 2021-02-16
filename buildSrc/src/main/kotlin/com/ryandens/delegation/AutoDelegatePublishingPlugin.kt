package com.ryandens.delegation

import nebula.plugin.publishing.maven.MavenDeveloperPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.publishing.maven.MavenScmPlugin
import nebula.plugin.publishing.maven.license.MavenApacheLicensePlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.plugin
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

/**
 * Configures projects for publishing to Maven Central.
 */
class AutoDelegatePublishingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        // apply all the necessary plugins for making the artifact acceptable to Sonatype
        target.apply {
            plugin<SigningPlugin>()
            plugin<MavenPublishPlugin>()
            plugin<SourceJarPlugin>()
            plugin<MavenScmPlugin>()
            plugin<MavenDeveloperPlugin>()
            plugin<MavenApacheLicensePlugin>()
            plugin<JavadocJarPlugin>()
        }

        // sign the publication produced by the Nebula MavenPublishPlugin
        target.configure<SigningExtension> {
            sign(target.extensions.getByType<PublishingExtension>().publications.getByName("nebula"))
        }
    }
}
