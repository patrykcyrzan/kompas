import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * `:feature:x:api` – public contract of a feature: navigation keys and deep link routes.
 * Lightweight on purpose: no Compose UI, no Hilt. Other features depend ONLY on this module.
 */
class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("skeleton-android-library"))
        pluginManager.apply(libs.pluginId("kotlin-serialization"))

        dependencies {
            "api"(project(":core:navigation"))
            "api"(project(":core:deeplink"))
        }
    }
}
