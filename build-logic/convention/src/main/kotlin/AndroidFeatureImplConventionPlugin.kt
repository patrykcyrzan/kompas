import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.library
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * `:feature:x:impl` – screens, ViewModels and the Hilt module contributing the feature's
 * entries to the navigation graph. Never depended upon by other features – only by `:app`.
 */
class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("skeleton-android-library"))
        pluginManager.apply(libs.pluginId("skeleton-android-compose"))
        pluginManager.apply(libs.pluginId("skeleton-hilt"))

        dependencies {
            "implementation"(project(":core:navigation"))
            "implementation"(project(":core:designsystem"))

            "implementation"(libs.library("androidx-compose-material3"))
            "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
            "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
            "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
        }
    }
}
