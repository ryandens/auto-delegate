plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(27))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Production artifacts target Java 11; tests use the JDK 27 toolchain defaults.
tasks.compileJava {
    options.release.set(11)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
