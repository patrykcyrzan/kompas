plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.pdfviewer.api)

    // The shared OkHttpClient – the download goes through the app's interceptors (Chucker in debug).
    implementation(projects.core.network)

    // rememberLauncherForActivityResult – Save goes through the system's "create document" picker.
    implementation(libs.androidx.activity.compose)
    // FileProvider – Share hands other apps a content:// URI, never a file path.
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
