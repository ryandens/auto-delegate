dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        gradlePluginPortal {
            content {
                includeGroupByRegex("com\\.netflix\\.nebula(\\..*)?")
                includeGroupByRegex("com\\.diffplug(\\..*)?")
                includeGroup("gradle.plugin.com.hierynomus")
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
