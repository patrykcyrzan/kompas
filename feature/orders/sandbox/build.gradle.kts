// Work on it alone: open sandboxes/orders in Android Studio.
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
}

dependencies {
    // Scenarios sign in through the fake session.
    implementation(projects.core.data)
}
