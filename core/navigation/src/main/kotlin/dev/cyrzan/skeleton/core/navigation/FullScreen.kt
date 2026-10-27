package dev.cyrzan.skeleton.core.navigation

/**
 * Entry metadata: this screen takes over the whole app – it opens a flow above the tabs and hides
 * the bottom bar (sign-in, onboarding, checkout...). Declared by the feature that owns the screen:
 *
 * ```
 * entry<LoginNavKey>(metadata = fullScreen()) { ... }
 * entry<FormStep1NavKey>(metadata = fullScreen() + sharedViewModelScope(FormStep1NavKey::class)) { ... }
 * ```
 *
 * Only the screen that opens the flow needs it – everything navigated to while a flow is open
 * stays in that flow anyway.
 */
fun fullScreen(): Map<String, Any> = mapOf(FULL_SCREEN_METADATA to true)

/** `true` for entries declared with [fullScreen]. */
fun Map<String, Any>.isFullScreen(): Boolean = this[FULL_SCREEN_METADATA] == true

private const val FULL_SCREEN_METADATA = "dev.cyrzan.skeleton.core.navigation.fullScreen"
