package dev.cyrzan.skeleton.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions

private val JAVA_VERSION = JavaVersion.VERSION_17
private val JVM_TARGET = JvmTarget.JVM_17

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
    }
    configureKotlin()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlin()
}

@Suppress("UNCHECKED_CAST")
private fun Project.configureKotlin() {
    // AGP 9 ships built-in Kotlin support, so the `kotlin` extension is present without applying kotlin-android.
    extensions.configure<KotlinBaseExtension>("kotlin") {
        (this as HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>).compilerOptions {
            jvmTarget.set(JVM_TARGET)
        }
    }
}
