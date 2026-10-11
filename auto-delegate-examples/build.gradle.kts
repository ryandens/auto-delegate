plugins {
    id("com.ryandens.delegation.java-conventions")
    id("com.ryandens.javaagent-test") version "0.12.2"
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation("org.mockito:mockito-core:5.24.0")
    testJavaagent("org.mockito:mockito-core")
}

tasks.test {
    // Mockito's agent appends to the bootstrap classpath, preventing class-data sharing.
    jvmArgs("-Xshare:off")
}
