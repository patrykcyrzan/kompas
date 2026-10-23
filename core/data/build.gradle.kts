plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.hilt)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)
}
