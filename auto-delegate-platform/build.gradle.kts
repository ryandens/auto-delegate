plugins {
    `java-platform`
    id("com.ryandens.delegation.publish")
}

description = """
    A Gradle platform, published as a Gradle Module Metadata and a Maven BOM, to help projects consume compatible versions of dependencies published by this project
""".trimIndent()

publishing {
    publications {
        named<MavenPublication>("nebula") {
            from(components["javaPlatform"])
        }
    }
}

dependencies {
    constraints {
        api("${project.group}:${project(":auto-delegate-annotations").name}:${project.version}")
        runtime("${project.group}:${project(":auto-delegate-processor").name}:${project.version}")
    }
}
