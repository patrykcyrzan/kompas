plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.home.api)
    // Only the *api* of other features – just their navigation keys.
    implementation(projects.feature.details.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.search.api)

    implementation(projects.core.data)
}
