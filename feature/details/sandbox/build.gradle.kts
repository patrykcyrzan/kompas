// Run with: ./gradlew -Pkompas.focus=details,search :feature:details:sandbox:installDebug
// (with just kompas.focus=details, search screens open as placeholders)
plugins {
    alias(libs.plugins.skeleton.android.sandbox)
    // PickerHarnessNavKey lives on the saved back stack.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // A real neighbour instead of the placeholder: category → search results → details again.
    // Optional: with kompas.focus=details the impl is not in the build and search stays a placeholder.
    findProject(":feature:search:impl")?.let { implementation(project(it.path)) }
    // A scenario starts on SearchResultsNavKey.
    implementation(projects.feature.search.api)

    // The picker harness screen.
    implementation(projects.core.designsystem)
}
