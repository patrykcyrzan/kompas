package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.rememberBackStackNavigator
import kotlinx.coroutines.flow.Flow

@Composable
internal fun SkeletonNavHost(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
    externalLinks: Flow<String>,
    modifier: Modifier = Modifier,
) {
    val backStackNavigator = rememberBackStackNavigator(startBackStack)
    val navigator = remember(backStackNavigator, deepLinkDispatcher) {
        AppNavigator(backStackNavigator, deepLinkDispatcher)
    }

    LaunchedEffect(navigator, externalLinks) {
        externalLinks.collect(navigator::openExternalLink)
    }

    NavDisplay(
        backStack = backStackNavigator.backStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            // Keeps rememberSaveable state per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes ViewModels to a single back stack entry (cleared on pop).
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entryProviderInstallers.forEach { installer ->
                with(installer) { install(navigator) }
            }
        },
    )
}
