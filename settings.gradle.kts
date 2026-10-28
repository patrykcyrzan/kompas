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

include(":app")

include(":core:model")
include(":core:data")
include(":core:network")
include(":core:deeplink")
include(":core:designsystem")
include(":core:navigation")

include(":feature:home:api")
include(":feature:home:impl")
include(":feature:details:api")
include(":feature:details:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:orders:api")
include(":feature:orders:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:support:api")
include(":feature:support:impl")
