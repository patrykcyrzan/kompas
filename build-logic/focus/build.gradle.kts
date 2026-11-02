import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A settings plugin – its own module (jar): loaded into the settings classloader, where the convention
// plugins could not see AGP if they were in the same jar.
plugins {
    `kotlin-dsl`
}

group = "dev.cyrzan.skeleton.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

gradlePlugin {
    plugins {
        register("focus") {
            id = "skeleton.focus"
            implementationClass = "FocusSettingsPlugin"
        }
    }
}
