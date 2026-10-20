plugins {
    alias(libs.plugins.skeleton.android.library)
    alias(libs.plugins.skeleton.android.compose)
}

dependencies {
    // Exposed as `api`: every feature works with NavKey / EntryProviderScope.
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.core)
}
