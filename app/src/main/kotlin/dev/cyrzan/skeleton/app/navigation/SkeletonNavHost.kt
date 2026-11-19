package dev.cyrzan.skeleton.app.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.app.analytics.Screen
import dev.cyrzan.skeleton.app.analytics.ScreenTracker
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.screenName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun SkeletonNavHost(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
    externalLinks: Flow<String>,
    isSignedIn: StateFlow<Boolean>,
    screenTracker: ScreenTracker,
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

    // The screen on top, for screen_viewed. The name comes from the entry's own metadata.
    LaunchedEffect(state, navigation.entryProvider, screenTracker) {
        snapshotFlow { state.currentEntryId?.let { id -> state.current?.let { key -> id to key } } }
            .filterNotNull()
            .collect { (id, key) ->
                val name = navigation.entryProvider(key).metadata.screenName()
                if (name == null) Log.w(TAG, "${key::class.simpleName} has no screenName() – not tracked")
                screenTracker.onScreenChanged(Screen(id, name))
            }
    }

    AppShell(navigation, modifier)
}

private const val TAG = "SkeletonNavHost"
