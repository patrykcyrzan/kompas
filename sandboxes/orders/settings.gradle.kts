/*
 * Orders sandbox – a separate, slim build: only the modules :feature:ordersdlew -p sandboxes/orders :feature:ordersths and directories are the same as in the full build. If the feature starts depending on
 * another module, Gradle reports "project ... not found" – add it to the list below.
 */
pluginManagement {
    includeBuild("../../build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kompas-orders-sandbox"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeFromRepo(
    ":core:model",
    ":core:data",
    ":core:network",
    ":core:deeplink",
    ":core:designsystem",
    ":core:navigation",
    ":sandbox",
)

includeFromRepo(
    ":feature:orders:api",
    ":feature:orders:impl",
    ":feature:orders:sandbox",
    // Keys only: orders navigates to these features, their screens open as placeholders.
    ":feature:details:api",
    ":feature:support:api",
)

/** Includes [paths] from the main repository, e.g. `:core:data` -> `../../core/data` (and `:core` -> `../../core`). */
fun includeFromRepo(vararg paths: String) = paths.forEach { path ->
    include(path)
    generateSequence(project(path)) { it.parent }.takeWhile { it != rootProject }.forEach { project ->
        project.projectDir = settingsDir.resolve("../.." + project.path.replace(':', '/'))
    }
}
