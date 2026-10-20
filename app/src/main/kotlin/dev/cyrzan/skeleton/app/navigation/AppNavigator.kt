package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.app.deeplink.toNavKey
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.navigation.BackStackNavigator
import dev.cyrzan.skeleton.core.navigation.Navigator

/** The [Navigator] features get: back stack operations, plus execution of deep link results. */
@Stable
internal class AppNavigator(
    private val backStack: BackStackNavigator,
    private val deepLinks: DeepLinkDispatcher,
) : Navigator {

    override fun navigate(key: NavKey) = backStack.navigate(key)

    override fun goBack(): Boolean = backStack.goBack()

    override fun resetTo(keys: List<NavKey>) = backStack.resetTo(keys)

    /** Link delivered by the OS while the app is running (`onNewIntent`). */
    fun openExternalLink(uri: String) {
        apply(deepLinks.dispatch(uri, DeepLinkSource.External), DeepLinkSource.External)
    }

    private fun apply(result: DeepLinkResult, source: DeepLinkSource) {
        when (result) {
            is DeepLinkResult.Navigate -> show(result.destination, result.backStack, source)
            is DeepLinkResult.Redirect -> show(result.destination, result.backStack, source)
            is DeepLinkResult.Rejected, is DeepLinkResult.Invalid, DeepLinkResult.NotMatched -> Unit
        }
    }

    /**
     * External link: the user comes from outside – show the whole synthetic back stack.
     * In-app link: the user is in the middle of something – push the target on top, so "back" returns them.
     */
    private fun show(destination: Destination, syntheticBackStack: List<Destination>, source: DeepLinkSource) {
        when (source) {
            DeepLinkSource.External -> backStack.resetTo(syntheticBackStack.map(Destination::toNavKey))
            DeepLinkSource.InApp -> {
                val key = destination.toNavKey()
                if (backStack.current != key) backStack.navigate(key)
            }
        }
    }
}
