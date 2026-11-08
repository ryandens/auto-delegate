plugins {
    `kotlin-dsl`
}

group = "com.ryandens.delegation"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(27))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
