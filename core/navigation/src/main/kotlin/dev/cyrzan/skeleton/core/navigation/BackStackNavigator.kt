package dev.cyrzan.skeleton.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/** [Navigator] backed by a saveable [NavBackStack] (survives config changes and process death). */
class BackStackNavigator(
    val backStack: NavBackStack<NavKey>,
) : Navigator {

    override fun navigate(key: NavKey) {
        backStack.add(key)
    }

    override fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    override fun resetTo(keys: List<NavKey>) {
        require(keys.isNotEmpty()) { "Back stack cannot be empty" }
        backStack.clear()
        backStack.addAll(keys)
    }
}

@Composable
fun rememberNavigator(startKey: NavKey): BackStackNavigator {
    val backStack = rememberNavBackStack(startKey)
    return remember(backStack) { BackStackNavigator(backStack) }
}
