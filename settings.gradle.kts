pluginManagement {
    includeBuild("build-logic")
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
}

rootProject.name = "Kompas"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

/*
 * Focus mode – work on a few features without configuring (or indexing) the rest of the app:
 *
 *   local.properties:  kompas.focus=orders            (Android Studio: sync after changing it)
 *   command line:      ./gradlew -Pkompas.focus=orders :feature:orders:sandbox:installDebug
 *
 * Kept: every :core module, every feature's :api (small, and what impls depend on) and, for the
 * focused features only, :impl and :sandbox. Left out: :app and all other impls – which is safe
 * because an impl never depends on another impl. Several features: kompas.focus=orders,details
 */
val features = listOf("home", "details", "settings", "orders", "profile", "auth", "search", "support")

val focus: Set<String> = (providers.gradleProperty("kompas.focus").orNull ?: localProperty("kompas.focus"))
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()
    .orEmpty()
require(features.containsAll(focus)) { "Unknown feature(s) in kompas.focus: ${focus - features.toSet()}" }

fun isFocused(feature: String) = focus.isEmpty() || feature in focus

if (focus.isEmpty()) include(":app")

include(":core:model")
include(":core:data")
include(":core:network")
include(":core:deeplink")
include(":core:designsystem")
include(":core:navigation")

include(":sandbox")

features.forEach { feature ->
    include(":feature:$feature:api")
    if (isFocused(feature)) {
        include(":feature:$feature:impl")
        if (settingsDir.resolve("feature/$feature/sandbox/build.gradle.kts").exists()) include(":feature:$feature:sandbox")
    }
}

fun localProperty(name: String): String? =
    providers.fileContents(layout.rootDirectory.file("local.properties")).asText.orNull
        ?.let { text -> java.util.Properties().apply { load(text.reader()) } }
        ?.getProperty(name)
