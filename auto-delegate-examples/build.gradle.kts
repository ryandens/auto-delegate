plugins {
    java
}

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.18.0")
}

tasks.test {
    useJUnitPlatform()
}
