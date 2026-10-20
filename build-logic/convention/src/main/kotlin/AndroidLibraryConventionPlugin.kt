import com.android.build.api.dsl.LibraryExtension
import dev.cyrzan.skeleton.buildlogic.configureKotlinAndroid
import dev.cyrzan.skeleton.buildlogic.defaultNamespace
import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.library
import dev.cyrzan.skeleton.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))

        extensions.configure<LibraryExtension> {
            namespace = defaultNamespace
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        dependencies {
            "testImplementation"(libs.library("junit"))
        }
    }
}
