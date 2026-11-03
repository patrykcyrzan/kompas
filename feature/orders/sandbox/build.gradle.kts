// Run with: ./gradlew :feature:orders:sandbox:installDebug
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
}

dependencies {
    // Scenarios sign in through the fake session.
    implementation(projects.core.data)
}
