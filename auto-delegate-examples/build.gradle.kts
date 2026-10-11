plugins {
    id("com.ryandens.delegation.java-conventions")
    java
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation("org.mockito:mockito-core:5.24.0")
}
