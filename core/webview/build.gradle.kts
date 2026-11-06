// The one way the app shows web content: a WebView with safe defaults, one page-load state machine
// and a URL policy features plug into. Features add rules (UrlHandler), never their own WebViewClient.
plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
}

dependencies {
    implementation(libs.androidx.compose.ui)
    // BackHandler – Back goes through the page's history first.
    implementation(libs.androidx.activity.compose)
    // WebView.onPause/onResume follow the screen's lifecycle.
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
