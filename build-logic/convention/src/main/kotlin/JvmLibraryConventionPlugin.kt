import dev.cyrzan.skeleton.buildlogic.configureKotlinJvm
import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.library
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-jvm"))
        configureKotlinJvm()

        dependencies {
            "testImplementation"(libs.library("junit"))
        }
    }
}
