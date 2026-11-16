plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
    // DTOs in data/remote.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.feature.invoices.api)

    // Only this feature calls the invoices API, so its Retrofit service and DTOs live here, not in :core:data.
    implementation(projects.core.network)
    implementation(projects.core.file)

    testImplementation(libs.kotlinx.coroutines.test)
}
