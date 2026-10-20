package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

/**
 * The single entry point features use to move around the app.
 *
 * Features never touch the back stack directly – it keeps navigation rules
 * (e.g. deep link / synthetic back stack handling) in one place.
 */
@Stable
interface Navigator {

    /** Pushes [key] on top of the back stack. */
    fun navigate(key: NavKey)

    /** Pops the top entry. Returns `false` when already at the root. */
    fun goBack(): Boolean

    /** Replaces the whole back stack, e.g. when handling a deep link. */
    fun resetTo(keys: List<NavKey>)
}
