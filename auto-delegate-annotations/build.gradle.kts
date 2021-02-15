plugins {
    `java-library`
    id("nebula.maven-publish")
    id("nebula.source-jar")
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}
