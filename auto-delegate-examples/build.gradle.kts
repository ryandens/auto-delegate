plugins {
    id("com.ryandens.delegation.java-conventions")
    alias(libs.plugins.javaagent.test)
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation(libs.mockito.core)
    testJavaagent(libs.mockito.core)
}

tasks.test {
    // Mockito's agent appends to the bootstrap classpath, preventing class-data sharing.
    jvmArgs("-Xshare:off")
}
