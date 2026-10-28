package dev.cyrzan.skeleton.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
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
    val navigation = rememberAppNavigation(entryProviderInstallers, deepLinkDispatcher, startBackStack)
    val state = navigation.state
    val navigator = navigation.navigator

    LaunchedEffect(navigator, externalLinks) {
        externalLinks.collect(navigator::openExternalLink)
    }

    // Sign-out: nothing of the previous user may stay on any stack (also on tabs nobody looks at).
    LaunchedEffect(state, isSignedIn) {
        isSignedIn.drop(1).filter { signedIn -> !signedIn }.collect { state.resetAll() }
    }

    AppShell(navigation, modifier)
}
