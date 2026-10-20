plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.ui)
}
