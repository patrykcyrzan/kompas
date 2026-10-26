plugins {
    alias(libs.plugins.skeleton.jvm.library)
    // Ids are navigation arguments: they must be serializable to survive process death.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.core)
}
