plugins {
    alias(libs.plugins.skeleton.android.feature.impl)
}

dependencies {
    implementation(projects.feature.orders.api)
    // Order items open the details screen.
    implementation(projects.feature.details.api)
    implementation(projects.feature.support.api)
    // Only the chat button (FAB) – the chat owns what it shows, where it leads and what it tracks.
    implementation(projects.feature.chat.api)

    implementation(projects.core.data)
}
