plugins {
    java
}

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor", "shadow"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.mockito:mockito-core:3.12.4")
}

tasks.test {
    useJUnitPlatform()
}
