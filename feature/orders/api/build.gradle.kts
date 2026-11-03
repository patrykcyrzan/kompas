plugins {
    alias(libs.plugins.skeleton.android.feature.api)
}

dependencies {
    // Typed ids are part of the keys (and so of this feature's public contract).
    api(projects.core.model)
}
