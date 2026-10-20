plugins {
    alias(libs.plugins.skeleton.android.application)
    alias(libs.plugins.skeleton.android.compose)
    alias(libs.plugins.skeleton.hilt)
}

android {
    namespace = "dev.cyrzan.skeleton.app"

    defaultConfig {
        applicationId = "dev.cyrzan.skeleton"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)

    // Start destination only.
    implementation(projects.feature.home.api)

    // impl modules are wired in purely through Hilt multibindings (Set<EntryProviderInstaller>).
    implementation(projects.feature.home.impl)
    implementation(projects.feature.details.impl)
    implementation(projects.feature.settings.impl)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
