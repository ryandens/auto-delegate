pluginManagement {
    includeBuild("test-conventions")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal {
            content {
                includeGroupByRegex("com\\.netflix\\.nebula(\\..*)?")
                includeGroup("com.diffplug.spotless")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("com\\.netflix\\.nebula(\\..*)?")
                excludeGroup("com.diffplug.spotless")
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
