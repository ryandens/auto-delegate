plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

dependencies {
    compileOnly(project(":auto-delegate-annotations"))
    annotationProcessor(project(":auto-delegate-processor"))
}