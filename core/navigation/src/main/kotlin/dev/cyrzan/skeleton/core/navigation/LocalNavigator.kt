package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [Navigator] of the host (the app or a sandbox), provided around its navigation display.
 *
 * Screens do NOT use it – they get callbacks from their entry and stay previewable without navigation.
 * It is for components one feature hands to other features' screens (e.g. the chat button), which
 * must navigate on their own because the screen embedding them does not know where they lead.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator – LocalNavigator is provided by the app's (or sandbox's) navigation host")
}
