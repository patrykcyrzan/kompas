package dev.cyrzan.skeleton.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.app.navigation.SkeletonNavHost
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    @Inject
    internal lateinit var deepLinkDispatcher: DeepLinkDispatcher

    /** Links arriving while the Activity is alive (singleTop -> onNewIntent). */
    private val externalLinks = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Cold start: the link becomes the *initial* back stack (no Home -> Details flash).
        // On recreation the saved back stack is restored, so the Intent is not processed again
        // (that would also re-save a pending link after a redirect).
        val linkBackStack = if (savedInstanceState == null) deepLinkDispatcher.startBackStackFor(intent) else null
        val startBackStack = linkBackStack ?: listOf(HomeNavKey)
        val links = externalLinks.receiveAsFlow()

        setContent {
            SkeletonTheme {
                SkeletonNavHost(
                    entryProviderInstallers = entryProviderInstallers,
                    deepLinkDispatcher = deepLinkDispatcher,
                    startBackStack = startBackStack,
                    externalLinks = links,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkDispatcher.uriOf(intent)?.let(externalLinks::trySend)
    }
}
