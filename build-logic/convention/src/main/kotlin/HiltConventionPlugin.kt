import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.library
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds Hilt with KSP. On Android modules also applies the Hilt Gradle plugin;
 * on pure JVM modules only `hilt-core` is added (no Android types available there).
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("ksp"))

        dependencies {
            "ksp"(libs.library("hilt-compiler"))
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            dependencies {
                "implementation"(libs.library("hilt-core"))
            }
        }

        pluginManager.withPlugin("com.android.base") {
            pluginManager.apply(libs.pluginId("hilt"))
            dependencies {
                "implementation"(libs.library("hilt-android"))
            }
        }
    }
}
