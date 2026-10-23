package dev.cyrzan.skeleton.app.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.rememberTabbedBackStack
import kotlinx.coroutines.flow.Flow

@Composable
internal fun SkeletonNavHost(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
    externalLinks: Flow<String>,
    modifier: Modifier = Modifier,
) {
    val tabs = rememberTabbedBackStack(TopLevelTab.roots, startBackStack)
    val navigator = remember(tabs, deepLinkDispatcher) { AppNavigator(tabs, deepLinkDispatcher) }

    LaunchedEffect(navigator, externalLinks) {
        externalLinks.collect(navigator::openExternalLink)
    }

    val entryProvider = entryProvider {
        entryProviderInstallers.forEach { installer ->
            with(installer) { install(navigator) }
        }
    }

    // Every tab is decorated all the time, also when hidden – so switching tabs keeps each
    // screen's saved UI state and ViewModels. Separate decorators per tab: the same key
    // (e.g. item #7) may sit on two tabs and must not share state.
    val entriesByTab = tabs.tabs.associateWith { tab ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = tabs.stackOf(tab).backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
    }
    // Start tab underneath the selected one: predictive back from another tab's root previews the start tab.
    val visibleEntries = entriesByTab.getValue(tabs.startTab) +
        if (tabs.currentTab != tabs.startTab) entriesByTab.getValue(tabs.currentTab) else emptyList()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tabs.currentTab == tab.root,
                        onClick = { tabs.selectTab(tab.root) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            entries = visibleEntries,
            // Screens have their own Scaffolds: let them skip the insets the bottom bar already handles.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            onBack = { navigator.goBack() },
        )
    }
}
