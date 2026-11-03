plugins {
    id("com.ryandens.delegation.java-conventions")
    alias(libs.plugins.javaagent.test)
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor")) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
    testImplementation(libs.mockito.core)
    testJavaagent(libs.mockito.core)
}

tasks.test {
    // Mockito's agent appends to the bootstrap classpath, preventing class-data sharing.
    jvmArgs("-Xshare:off")
}
