package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.result.ResultEventBus
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.NavigationState
import dev.cyrzan.skeleton.core.navigation.isFullScreen
import dev.cyrzan.skeleton.core.navigation.rememberAppResultEventBus
import dev.cyrzan.skeleton.core.navigation.rememberNavigationState

/** Everything the app's navigation is made of, created together by [rememberAppNavigation]. */
@Stable
internal class AppNavigation(
    val state: NavigationState,
    val navigator: AppNavigator,
    val entryProvider: (NavKey) -> NavEntry<NavKey>,
    val resultEventBus: ResultEventBus,
)

/**
 * Creates the navigation in the only order that works – kept in one place so it cannot be split or reordered:
 *
 * 1. the navigator – features capture it in their entries (they only call it later, from callbacks),
 * 2. the entries – built with that navigator,
 * 3. the state – asks the entries which screens are full screen (also for [startBackStack]),
 * 4. the state is attached to the navigator, before any screen can call it.
 */
@Composable
internal fun rememberAppNavigation(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
): AppNavigation {
    val resultEventBus = rememberAppResultEventBus()
    val navigator = remember(deepLinkDispatcher, resultEventBus) { AppNavigator(deepLinkDispatcher, resultEventBus) }
    val entries = remember(entryProviderInstallers, navigator) {
        entryProvider {
            entryProviderInstallers.forEach { installer ->
                with(installer) { install(navigator) }
            }
        }
    }
    // Each feature declares on its own entries which screens open a flow above the tabs.
    val state = rememberNavigationState(TopLevelTab.roots, startBackStack) { key -> entries(key).metadata.isFullScreen() }
    return remember(navigator, entries, state) {
        navigator.attach(state)
        AppNavigation(state, navigator, entries, resultEventBus)
    }
}
