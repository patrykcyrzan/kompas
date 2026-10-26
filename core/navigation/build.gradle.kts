plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
}

dependencies {
    // Exposed as `api`: every feature works with NavKey / EntryProviderScope.
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.core)

    // AppNavDisplay: the Nav3 UI wiring (decorators, NavDisplay) lives here so `:app` only describes its tabs.
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
}
