plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.chat.api)

    // ChatInitializer – the SDK is initialized with the app, without :app knowing it exists.
    implementation(projects.core.startup)
    // chat_button_clicked.
    implementation(projects.core.analytics)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
