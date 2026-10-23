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
    implementation(projects.core.data)

    // Tab roots and the deep link hierarchy (which tab a screen belongs to) are app-level decisions.
    implementation(projects.feature.home.api)
    implementation(projects.feature.details.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.orders.api)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.auth.api)

    // impl modules are wired in purely through Hilt multibindings (Set<EntryProviderInstaller>).
    implementation(projects.feature.home.impl)
    implementation(projects.feature.details.impl)
    implementation(projects.feature.settings.impl)
    implementation(projects.feature.orders.impl)
    implementation(projects.feature.profile.impl)
    implementation(projects.feature.auth.impl)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    testImplementation(libs.junit)
}
