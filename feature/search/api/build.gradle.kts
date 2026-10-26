plugins {
    alias(libs.plugins.skeleton.android.feature.api)
}

dependencies {
    // ItemCategory is part of SearchCriteria (and so of this feature's public contract).
    api(projects.core.model)
}
