plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.orders.api)
    // Order items open the details screen.
    implementation(projects.feature.details.api)

    implementation(projects.core.data)
}
