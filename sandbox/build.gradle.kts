// The shared part of every `:feature:x:sandbox` app. Never a dependency of `:app`.
plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
    alias(libs.plugins.skeleton.hilt)
    // SandboxHomeNavKey lives on the saved back stack.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Exposed as `api`: a sandbox declares its scenarios with NavKeys.
    api(projects.core.navigation)

    implementation(projects.core.designsystem)
    implementation(projects.core.deeplink)
    implementation(projects.core.data)
    implementation(projects.core.network)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
