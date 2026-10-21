package dev.cyrzan.skeleton.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.deeplink.DeepLinkDispatcher
import dev.cyrzan.skeleton.app.deeplink.LegacyDeepLinkNavigator
import dev.cyrzan.skeleton.app.legacy.DeepLinkHost
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Legacy single Activity: NavHostFragment + nav_graph.xml. Deep links go through the same
 * platform-neutral resolver as in the Compose app – only the executor differs.
 */
@AndroidEntryPoint
internal class MainActivity : FragmentActivity(R.layout.activity_main), DeepLinkHost {

    @Inject
    internal lateinit var deepLinkDispatcher: DeepLinkDispatcher

    override lateinit var deepLinks: LegacyDeepLinkNavigator
        private set

    /** Links arriving from the OS. Applied only while STARTED – never after onSaveInstanceState. */
    private val externalLinks = Channel<String>(Channel.CONFLATED)

    private val activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Login finished -> open the link it interrupted (validated again). Cancelled -> forget it.
        if (result.resultCode == Activity.RESULT_OK) deepLinks.resumePending() else deepLinkDispatcher.discardPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        deepLinks = LegacyDeepLinkNavigator(
            context = this,
            navController = navHost.navController,
            dispatcher = deepLinkDispatcher,
            startActivity = activityLauncher::launch,
        )

        // On recreation NavController restores its own stack – the Intent must not be applied again.
        if (savedInstanceState == null) deepLinkDispatcher.uriOf(intent)?.let(externalLinks::trySend)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                externalLinks.receiveAsFlow().collect(deepLinks::openExternal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkDispatcher.uriOf(intent)?.let(externalLinks::trySend)
    }
}
