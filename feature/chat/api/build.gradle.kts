plugins {
    alias(libs.plugins.skeleton.android.feature.api)
    // The exception among the apis: besides its key, chat hands other features a ready-made UI component (ChatButton).
    alias(libs.plugins.skeleton.android.compose)
}
