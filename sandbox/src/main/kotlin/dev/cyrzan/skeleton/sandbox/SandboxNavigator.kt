package dev.cyrzan.skeleton.sandbox

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.navigation.NavigationState
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.core.navigation.ResultKey

/**
 * The [Navigator] a feature gets in its sandbox. Same contract as the app's one, with a single tab
 * (the launcher) and no guards: links only resolve to the sandboxed feature's routes and simply open
 * their target – the session is switched on the launcher instead.
 */
@Stable
internal class SandboxNavigator(
    private val deepLinks: DeepLinkResolver,
    private val results: ResultEventBus,
) : Navigator {

    private var attached: NavigationState? = null

    private val state: NavigationState
        get() = checkNotNull(attached) { "Navigator used before the navigation state was attached" }

    fun attach(state: NavigationState) {
        attached = state
    }

    fun start(scenario: Scenario) {
        scenario.prepare()
        state.resetTo(listOf(SandboxHomeNavKey) + scenario.backStack)
    }

    override fun navigate(key: NavKey) = state.navigate(key)

    override fun goBack(): Boolean = state.goBack()

    override fun <T : Any> goBackWithResult(key: ResultKey<T>, result: T) {
        results.sendResult(key.name, result)
        state.goBack()
    }

    override fun closeFlow() = state.closeFlow()

    override fun openLink(uri: String): OpenLinkResult =
        when (val result = deepLinks.resolve(uri, DeepLinkSource.InApp)) {
            is DeepLinkResult.Navigate -> {
                state.navigate(result.destination as NavKey)
                OpenLinkResult.Opened
            }
            is DeepLinkResult.Rejected, is DeepLinkResult.Redirect -> OpenLinkResult.Blocked
            // Other features' links are not part of the sandbox.
            is DeepLinkResult.Invalid, DeepLinkResult.NotMatched -> OpenLinkResult.Unsupported
        }

    /** Nothing is ever interrupted by a redirect here. */
    override fun resumePendingLink(): Boolean = false
}
