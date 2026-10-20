import com.android.build.api.dsl.CommonExtension
import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.library
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Enables Jetpack Compose on an Android application or library module. Apply after the android plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-compose"))

        extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true

        dependencies {
            val bom = libs.library("androidx-compose-bom")
            "implementation"(platform(bom))
            "androidTestImplementation"(platform(bom))
            "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
            "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
        }
    }
}
