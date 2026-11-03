// Run with: ./gradlew :feature:details:sandbox:installDebug
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
    // PickerHarnessNavKey lives on the saved back stack.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // A real neighbour instead of the placeholder: category → search results → details again.
    implementation(projects.feature.search.impl)
    // A scenario starts on SearchResultsNavKey.
    implementation(projects.feature.search.api)

    // The picker harness screen.
    implementation(projects.core.designsystem)
}
