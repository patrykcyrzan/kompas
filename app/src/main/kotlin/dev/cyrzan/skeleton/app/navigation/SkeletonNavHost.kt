package dev.cyrzan.skeleton.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.AppNavDisplay
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.isFullScreen
import dev.cyrzan.skeleton.core.navigation.rememberNavigationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

@Composable
internal fun SkeletonNavHost(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
    externalLinks: Flow<String>,
    isSignedIn: StateFlow<Boolean>,
    modifier: Modifier = Modifier,
) {
    val navigator = remember(deepLinkDispatcher) { AppNavigator(deepLinkDispatcher) }
    val entries = remember(entryProviderInstallers, navigator) {
        entryProvider {
            entryProviderInstallers.forEach { installer ->
                with(installer) { install(navigator) }
            }
        }
    }
    // Each feature declares on its own entries which screens open a flow above the tabs.
    val state = rememberNavigationState(TopLevelTab.roots, startBackStack) { key -> entries(key).metadata.isFullScreen() }
    navigator.state = state

    LaunchedEffect(navigator, externalLinks) {
        externalLinks.collect(navigator::openExternalLink)
    }

    // Sign-out: nothing of the previous user may stay on any stack (also on tabs nobody looks at).
    LaunchedEffect(state, isSignedIn) {
        isSignedIn.drop(1).filter { signedIn -> !signedIn }.collect { state.resetAll() }
    }

    AppNavDisplay(
        state = state,
        entryProvider = entries,
        bottomBar = {
            NavigationBar {
                TopLevelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tabs.currentTab == tab.root,
                        onClick = { state.selectTab(tab.root) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        modifier = modifier,
        onBack = { navigator.goBack() },
    )
}
