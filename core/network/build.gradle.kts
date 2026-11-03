plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.hilt)
}

dependencies {
    // Part of the public surface: ExtraInterceptors takes OkHttp interceptors, data modules create Retrofit services.
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.kotlinx.serialization.json)

    implementation(libs.retrofit.converter.kotlinx.serialization)

    // Only src/debug references it.
    debugImplementation(libs.okhttp.logging.interceptor)
}
