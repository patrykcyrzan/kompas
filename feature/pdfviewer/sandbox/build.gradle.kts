// Run with: ./gradlew :feature:pdfviewer:sandbox:installDebug
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
    // WebViewHarnessNavKey lives on the saved back stack.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // The WebView harness screen.
    implementation(projects.core.designsystem)
    implementation(projects.core.webview)
}
