plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.webpage.api)
    implementation(projects.core.webview)

    // A PDF link in the page opens in the app's viewer.
    implementation(projects.feature.pdfviewer.api)
}
