plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.details.api)
    implementation(projects.feature.search.api)

    implementation(projects.core.data)
}
