plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.auth.api)

    implementation(projects.core.data)
}
