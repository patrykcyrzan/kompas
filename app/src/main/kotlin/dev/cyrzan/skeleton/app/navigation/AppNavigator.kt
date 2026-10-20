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
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult

/** The [Navigator] features get: back stack operations + links, all going through the deep link pipeline. */
@Stable
internal class AppNavigator(
    private val backStack: BackStackNavigator,
    private val deepLinks: DeepLinkDispatcher,
) : Navigator {

    override fun navigate(key: NavKey) = backStack.navigate(key)

    override fun goBack(): Boolean = backStack.goBack()

    override fun resetTo(keys: List<NavKey>) = backStack.resetTo(keys)

    override fun openLink(uri: String): OpenLinkResult =
        apply(deepLinks.dispatch(uri, DeepLinkSource.InApp), DeepLinkSource.InApp)

    /** Link delivered by the OS while the app is running (`onNewIntent`). */
    fun openExternalLink(uri: String) {
        apply(deepLinks.dispatch(uri, DeepLinkSource.External), DeepLinkSource.External)
    }

    override fun resumePendingLink(): Boolean {
        val (source, result) = deepLinks.resumePending() ?: return false
        return apply(result, source) == OpenLinkResult.Opened
    }

    private fun apply(result: DeepLinkResult, source: DeepLinkSource): OpenLinkResult = when (result) {
        is DeepLinkResult.Navigate -> show(result.destination, result.backStack, source)
        is DeepLinkResult.Redirect -> show(result.destination, result.backStack, source)
        is DeepLinkResult.Rejected -> OpenLinkResult.Blocked
        is DeepLinkResult.Invalid, DeepLinkResult.NotMatched -> OpenLinkResult.Unsupported
    }

    /**
     * External link: the user comes from outside – show the whole synthetic back stack.
     * In-app link: the user is in the middle of something – push the target on top, so "back" returns them.
     */
    private fun show(destination: Destination, syntheticBackStack: List<Destination>, source: DeepLinkSource): OpenLinkResult {
        when (source) {
            DeepLinkSource.External -> backStack.resetTo(syntheticBackStack.map(Destination::toNavKey))
            DeepLinkSource.InApp -> {
                val key = destination.toNavKey()
                if (backStack.current != key) backStack.navigate(key)
            }
        }
        return OpenLinkResult.Opened
    }
}
