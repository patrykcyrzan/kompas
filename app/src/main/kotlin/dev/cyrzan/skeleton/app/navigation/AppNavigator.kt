package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.core.navigation.BackStackNavigator
import dev.cyrzan.skeleton.core.navigation.Navigator

/** The [Navigator] features get. */
@Stable
internal class AppNavigator(
    private val backStack: BackStackNavigator,
) : Navigator {

    override fun navigate(key: NavKey) = backStack.navigate(key)

    override fun goBack(): Boolean = backStack.goBack()

    override fun resetTo(keys: List<NavKey>) = backStack.resetTo(keys)
}
