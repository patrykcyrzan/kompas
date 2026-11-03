plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.search.api)
    // Results open item details.
    implementation(projects.feature.details.api)

    implementation(projects.core.data)
}
dependencies { testImplementation(libs.junit) }
