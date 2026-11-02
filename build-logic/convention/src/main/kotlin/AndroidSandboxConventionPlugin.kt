import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import dev.cyrzan.skeleton.buildlogic.defaultNamespace
import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * `:feature:x:sandbox` – a tiny app that runs ONE feature without `:app`: its screens, its fake data,
 * a launcher with scenarios, and placeholders for other features' screens.
 *
 * Wires in the feature's own `api` + `impl` (derived from the path) and `:sandbox`, gets its own
 * applicationId (installs next to the real app) and has no release variant.
 */
class AndroidSandboxConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val segments = path.split(':')
        require(segments.size == 4 && segments[1] == "feature" && segments[3] == "sandbox") {
            "skeleton.android.sandbox is meant for :feature:<name>:sandbox, not $path"
        }
        val feature = segments[2]

        pluginManager.apply(libs.pluginId("skeleton-android-application"))
        pluginManager.apply(libs.pluginId("skeleton-android-compose"))
        pluginManager.apply(libs.pluginId("skeleton-hilt"))

        extensions.configure<ApplicationExtension> {
            namespace = defaultNamespace
            defaultConfig.applicationId = "dev.cyrzan.skeleton.sandbox." + feature.replace('-', '_')
            // Used by the launcher label in :sandbox's manifest.
            defaultConfig.manifestPlaceholders["sandboxLabel"] = feature.replaceFirstChar(Char::uppercase) + " sandbox"
        }

        // A developer tool: building release would only double the work of `assemble`.
        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants { variant -> if (variant.buildType == "release") variant.enable = false }
        }

        dependencies {
            "implementation"(project(":sandbox"))
            "implementation"(project(":feature:$feature:api"))
            "implementation"(project(":feature:$feature:impl"))
        }
    }
}
