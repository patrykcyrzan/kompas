plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.profile.api)
    implementation(projects.feature.orders.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.auth.api)
    implementation(projects.feature.details.api)

    implementation(projects.core.data)
}
