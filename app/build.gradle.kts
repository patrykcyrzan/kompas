plugins {
    alias(libs.plugins.skeleton.android.application)
    alias(libs.plugins.skeleton.android.compose)
    alias(libs.plugins.skeleton.hilt)
}

android {
    namespace = "dev.cyrzan.skeleton.app"

    buildFeatures {
        buildConfig = true
    }

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
    implementation(projects.core.deeplink)

    // New Compose features, shown inside the legacy app through ComposeHostFragment.
    // (:feature:home is not wired here – in this simulation Home is still a legacy Fragment.)
    implementation(projects.feature.details.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.details.impl)
    implementation(projects.feature.settings.impl)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // The "old" navigation of the monolith.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment)

    testImplementation(libs.junit)
}
