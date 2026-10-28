plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
}

dependencies {
    // Exposed as `api`: every feature works with NavKey / EntryProviderScope.
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.core)

    // NavigationDisplay: the Nav3 wiring (decorators, entry identity, NavDisplay). The shell around it is in `:app`.
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
