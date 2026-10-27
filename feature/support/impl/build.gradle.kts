plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.support.api)

    implementation(projects.core.data)

    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
