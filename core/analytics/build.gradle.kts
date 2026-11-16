plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.hilt)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.kotlinx.coroutines.test)
}
