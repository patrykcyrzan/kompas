package dev.cyrzan.skeleton.app.navigation

import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.feature.auth.api.LoginNavKey

/**
 * Screens that take over the whole app (no bottom bar) and form a flow above the tabs – e.g. sign-in,
 * onboarding, checkout. An app-level decision: features only call `navigator.navigate(key)`.
 */
internal fun isFullScreen(key: NavKey): Boolean = key is LoginNavKey
