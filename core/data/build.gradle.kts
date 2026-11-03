plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.hilt)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    // Retrofit services and DTOs live here; features only see the repositories.
    implementation(projects.core.network)
    implementation(libs.androidx.core.ktx)
}
