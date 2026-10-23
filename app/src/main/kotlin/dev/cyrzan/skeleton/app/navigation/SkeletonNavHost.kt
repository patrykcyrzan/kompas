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
import dev.cyrzan.skeleton.core.navigation.rememberNavigationState
import kotlinx.coroutines.flow.Flow

@Composable
internal fun SkeletonNavHost(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
    externalLinks: Flow<String>,
    modifier: Modifier = Modifier,
) {
    val state = rememberNavigationState(TopLevelTab.roots, startBackStack, ::isFullScreen)
    val navigator = remember(state, deepLinkDispatcher) { AppNavigator(state, deepLinkDispatcher) }

    LaunchedEffect(navigator, externalLinks) {
        externalLinks.collect(navigator::openExternalLink)
    }

    AppNavDisplay(
        state = state,
        entryProvider = entryProvider {
            entryProviderInstallers.forEach { installer ->
                with(installer) { install(navigator) }
            }
        },
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
