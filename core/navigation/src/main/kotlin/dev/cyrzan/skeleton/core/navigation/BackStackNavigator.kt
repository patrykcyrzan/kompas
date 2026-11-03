package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Low-level back stack operations over a saveable [NavBackStack] (survives config changes and
 * process death). Features use [Navigator]; this is what the app's [Navigator] implementation builds on.
 */
@Stable
class BackStackNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    val current: NavKey? get() = backStack.lastOrNull()

    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /**
     * Keeps the common prefix untouched, so entries that stay on the stack (e.g. Home)
     * keep their ViewModels and saved UI state.
     */
    fun resetTo(keys: List<NavKey>) {
        require(keys.isNotEmpty()) { "Back stack cannot be empty" }
        backStack.replaceWith(keys)
    }
}

/** Replaces the content with [keys], leaving the common prefix (and its state) untouched. */
internal fun MutableList<NavKey>.replaceWith(keys: List<NavKey>) {
    val commonPrefix = zip(keys).takeWhile { (current, new) -> current == new }.size
    while (size > commonPrefix) removeAt(lastIndex)
    addAll(keys.drop(commonPrefix))
}

/**
 * [startBackStack] is used only on first creation. After a configuration change or process death
 * the saved back stack is restored instead – so a deep link Intent is never applied twice.
 */
@Composable
fun rememberBackStackNavigator(startBackStack: List<NavKey>): BackStackNavigator {
    require(startBackStack.isNotEmpty()) { "Back stack cannot be empty" }
    val backStack = rememberNavBackStack(*startBackStack.toTypedArray())
    return remember(backStack) { BackStackNavigator(backStack) }
}
