plugins {
    `java-library`
    id("nebula.maven-publish") version ("17.3.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}
