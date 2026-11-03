// Platform-neutral deep link logic. Deliberately pure Kotlin with no Android, Compose, Nav3 or java.* APIs,
// so it is fully testable with plain JVM unit tests and independent of any platform or navigation library.
plugins {
    alias(libs.plugins.skeleton.jvm.library)
}
