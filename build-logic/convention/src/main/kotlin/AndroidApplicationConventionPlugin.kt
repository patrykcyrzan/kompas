import com.android.build.api.dsl.ApplicationExtension
import dev.cyrzan.skeleton.buildlogic.configureKotlinAndroid
import dev.cyrzan.skeleton.buildlogic.libs
import dev.cyrzan.skeleton.buildlogic.pluginId
import dev.cyrzan.skeleton.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.version("targetSdk").toInt()
        }
    }
}
