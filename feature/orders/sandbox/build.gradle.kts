// Run with: ./gradlew :feature:orders:sandbox:installDebug (add kompas.focus=orders to local.properties to load only this feature).
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
}

dependencies {
    // Scenarios sign in through the fake session.
    implementation(projects.core.data)
}
