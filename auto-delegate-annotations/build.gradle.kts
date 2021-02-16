plugins {
    `java-library`
    signing
    id("nebula.maven-publish")
    id("nebula.source-jar")
    id("nebula.maven-scm")
    id("nebula.maven-developer")
    id("nebula.maven-apache-license")
    id("nebula.javadoc-jar")
}

signing {
    sign(publishing.publications["nebula"])
}

description = """
    Annotations and metadata for decorating classes to enable the generation of auto-delegating abstract implementations of interfaces.
""".trimIndent()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
