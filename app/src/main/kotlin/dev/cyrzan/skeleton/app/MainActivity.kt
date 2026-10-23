package dev.cyrzan.skeleton.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.app.lock.AppLock
import dev.cyrzan.skeleton.app.lock.LockScreen
import dev.cyrzan.skeleton.app.navigation.SkeletonNavHost
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
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

    @Inject
    lateinit var session: SessionRepository

    @Inject
    internal lateinit var appLock: AppLock

    /** Links arriving while the Activity is alive (singleTop -> onNewIntent). */
    private val externalLinks = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The recents thumbnail would show the screen the lock is meant to hide.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)

        // Cold start: the link becomes the *initial* back stack (no Home -> Details flash).
        // On recreation the saved back stack is restored, so the Intent is not processed again
        // (that would also re-save a pending link after a redirect).
        val linkBackStack = if (savedInstanceState == null) deepLinkDispatcher.startBackStackFor(intent) else null
        val startBackStack = linkBackStack ?: listOf(HomeNavKey)
        val links = externalLinks.receiveAsFlow()

        setContent {
            SkeletonTheme {
                // Collected also while stopped: a lock set in the background is composed before the first frame back.
                val isLocked by appLock.isLocked.collectAsState()
                Box(Modifier.fillMaxSize()) {
                    // Stays composed under the lock: back stacks, ViewModels and scroll positions survive.
                    // Links arriving while locked are applied underneath and visible after unlocking.
                    SkeletonNavHost(
                        entryProviderInstallers = entryProviderInstallers,
                        deepLinkDispatcher = deepLinkDispatcher,
                        startBackStack = startBackStack,
                        externalLinks = links,
                        isSignedIn = session.isSignedIn,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isLocked) Modifier.clearAndSetSemantics {} else Modifier),
                    )
                    if (isLocked) LockScreen(appLock)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkDispatcher.uriOf(intent)?.let(externalLinks::trySend)
    }
}
