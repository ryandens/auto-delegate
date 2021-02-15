plugins {
    `java-library`
    id("nebula.maven-publish")
    id("nebula.source-jar")
    id("nebula.maven-scm")
    id("nebula.maven-developer")
    id("nebula.maven-apache-license")
    id("nebula.javadoc-jar")
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}
