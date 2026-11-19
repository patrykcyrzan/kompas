package dev.cyrzan.skeleton.core.navigation

/**
 * Entry metadata: the name the screen is reported under in analytics (`screen_viewed`). Declared by
 * the feature that owns the screen, next to its other metadata:
 *
 * ```
 * entry<HomeNavKey>(metadata = screenName("home")) { ... }
 * entry<LoginNavKey>(metadata = fullScreen() + screenName("login")) { ... }
 * ```
 *
 * A fixed string, not the key's class name – that one is obfuscated in release and changes with refactors,
 * and dashboards must not. Never put arguments in it (item ids, queries): the name is sent without consent.
 */
fun screenName(name: String): Map<String, Any> = mapOf(SCREEN_NAME_METADATA to name)

/** The name declared with [screenName], `null` when the entry has none. */
fun Map<String, Any>.screenName(): String? = this[SCREEN_NAME_METADATA] as? String

private const val SCREEN_NAME_METADATA = "dev.cyrzan.skeleton.core.navigation.screenName"
