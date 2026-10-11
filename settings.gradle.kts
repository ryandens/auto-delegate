pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("com.gradle.develocity") version "4.6.0"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { System.getenv("CI") == "true" }
    }
}

rootProject.name = "auto-delegate"
include("auto-delegate-annotations", "auto-delegate-processor", "auto-delegate-examples")
