plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

dependencies {
    implementation("com.squareup", "javapoet", "1.13.0")
    implementation("com.google.auto", "auto-common", "0.11")
    implementation(project(":auto-delegate-annotations"))
    val autoServiceVersion = "1.0-rc7"
    compileOnly("com.google.auto.service", "auto-service-annotations", autoServiceVersion)
    annotationProcessor("com.google.auto.service", "auto-service", autoServiceVersion)
}
