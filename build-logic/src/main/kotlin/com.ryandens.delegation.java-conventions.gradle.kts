import org.gradle.api.attributes.java.TargetJvmVersion

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
    options.release.set(11)
    options.encoding = "UTF-8"
}

// Tests emit Java 11 bytecode but run on the toolchain JVM, allowing JUnit 6's Java 17+ runtime.
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name) {
        attributes.attributeProvider(
            TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
            java.toolchain.languageVersion.map { it.asInt() },
        )
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
