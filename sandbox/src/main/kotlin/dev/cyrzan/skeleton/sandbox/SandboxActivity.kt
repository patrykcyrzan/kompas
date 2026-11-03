package dev.cyrzan.skeleton.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.NavigationDisplay
import dev.cyrzan.skeleton.core.navigation.isFullScreen
import dev.cyrzan.skeleton.core.navigation.rememberAppResultEventBus
import dev.cyrzan.skeleton.core.navigation.rememberNavigationState
import javax.inject.Inject

/**
 * The only screen host of a sandbox app. Collects the same `Set<EntryProviderInstaller>` as `:app` –
 * but on the classpath there is just the sandboxed feature's impl, so only its screens are real;
 * every other key opens [NotInSandboxScreen].
 */
@AndroidEntryPoint
class SandboxActivity : ComponentActivity() {

    @Inject
    lateinit var sandbox: Sandbox

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    @Inject
    lateinit var deepLinkResolver: DeepLinkResolver

    @Inject
    lateinit var session: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkeletonTheme {
                SandboxNavHost(sandbox, entryProviderInstallers, deepLinkResolver, session)
            }
        }
    }
}

@Composable
private fun SandboxNavHost(
    sandbox: Sandbox,
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkResolver: DeepLinkResolver,
    session: SessionRepository,
) {
    // Same order as in the app (rememberAppNavigation): navigator → entries → state → attach.
    val resultEventBus = rememberAppResultEventBus()
    val navigator = remember(deepLinkResolver, resultEventBus) { SandboxNavigator(deepLinkResolver, resultEventBus) }
    val entries = remember(sandbox, entryProviderInstallers, navigator) {
        entryProvider(fallback = { key -> NavEntry(key) { NotInSandboxScreen(key, onBack = { navigator.goBack() }) } }) {
            entry<SandboxHomeNavKey> { SandboxHomeScreen(sandbox, session, onScenarioClick = navigator::start) }
            entryProviderInstallers.forEach { installer ->
                with(installer) { install(navigator) }
            }
        }
    }
    // The launcher is the only tab; scenarios and everything they open are pushed above it.
    val tabs = listOf<NavKey>(SandboxHomeNavKey)
    val state = rememberNavigationState(tabs, tabs) { key -> entries(key).metadata.isFullScreen() }
    remember(navigator, state) { navigator.attach(state) }

    NavigationDisplay(
        state = state,
        entryProvider = entries,
        modifier = Modifier.fillMaxSize(),
        resultEventBus = resultEventBus,
        onBack = { navigator.goBack() },
    )
}
