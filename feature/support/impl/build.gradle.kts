plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.feature.support.api)

    implementation(projects.core.data)

    testImplementation(libs.junit)
}
