plugins {
    java
}

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.14.1")
}

tasks.test {
    useJUnitPlatform()
}
