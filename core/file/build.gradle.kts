plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.hilt)
}

dependencies {
    // FileProvider – files leave the app as content:// URIs, never as paths.
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
