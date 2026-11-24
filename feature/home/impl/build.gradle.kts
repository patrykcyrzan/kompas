plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.home.api)
    // Only the *api* of other features – just their navigation keys.
    implementation(projects.feature.details.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.search.api)
    // Only the chat button (FAB) – the chat owns what it shows, where it leads and what it tracks.
    implementation(projects.feature.chat.api)

    implementation(projects.core.data)
}
