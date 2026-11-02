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
val focus: Set<String> = (providers.gradleProperty("kompas.focus").orNull ?: localProperty("kompas.focus"))
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()
    .orEmpty()
require(focus.all { settingsDir.resolve("feature/$it").isDirectory }) { "Unknown feature(s) in kompas.focus: $focus" }

fun isFocused(feature: String) = focus.isEmpty() || feature in focus

if (focus.isEmpty()) include(":app")

include(":core:model")
include(":core:data")
include(":core:network")
include(":core:deeplink")
include(":core:designsystem")
include(":core:navigation")

include(":sandbox")

include(":feature:home:api")
if (isFocused("home")) include(":feature:home:impl")
include(":feature:details:api")
if (isFocused("details")) include(":feature:details:impl", ":feature:details:sandbox")
include(":feature:settings:api")
if (isFocused("settings")) include(":feature:settings:impl")
include(":feature:orders:api")
if (isFocused("orders")) include(":feature:orders:impl", ":feature:orders:sandbox")
include(":feature:profile:api")
if (isFocused("profile")) include(":feature:profile:impl")
include(":feature:auth:api")
if (isFocused("auth")) include(":feature:auth:impl")
include(":feature:search:api")
if (isFocused("search")) include(":feature:search:impl")
include(":feature:support:api")
if (isFocused("support")) include(":feature:support:impl")

fun localProperty(name: String): String? =
    providers.fileContents(layout.rootDirectory.file("local.properties")).asText.orNull
        ?.let { text -> java.util.Properties().apply { load(text.reader()) } }
        ?.getProperty(name)
