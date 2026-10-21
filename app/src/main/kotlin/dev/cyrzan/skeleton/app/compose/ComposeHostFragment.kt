package dev.cyrzan.skeleton.app.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import dagger.hilt.android.AndroidEntryPoint
import dev.cyrzan.skeleton.app.R
import dev.cyrzan.skeleton.app.deeplink.LegacyDeepLinkNavigator
import dev.cyrzan.skeleton.app.legacy.deepLinks
import dev.cyrzan.skeleton.core.designsystem.theme.SkeletonTheme
import dev.cyrzan.skeleton.core.navigation.BackStackNavigator
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.core.navigation.rememberBackStackNavigator
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Inject

/**
 * The bridge between the legacy NavController world and new Compose features.
 *
 * One destination in nav_graph.xml hosts a Nav3 [NavDisplay] with all feature entries. The initial
 * keys come from the Fragment arguments; afterwards the Nav3 back stack is saved like in the pure
 * Compose app. Features see a regular [Navigator] and don't know they live inside a Fragment.
 */
@AndroidEntryPoint
class ComposeHostFragment : Fragment() {

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val startKeys = decodeFromSavedState(KEYS_SERIALIZER, requireArguments().getBundle(ARG_KEYS)!!)
        val navController = findNavController()
        val deepLinks = deepLinks

        return ComposeView(requireContext()).apply {
            // Stable id: rememberSaveable (and so the Nav3 back stack) is saved per view id.
            id = R.id.compose_host
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SkeletonTheme {
                    val backStack = rememberBackStackNavigator(startKeys)
                    val navigator = remember(backStack) { HostedNavigator(backStack, navController, deepLinks) }
                    NavDisplay(
                        backStack = backStack.backStack,
                        onBack = { navigator.goBack() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entryProviderInstallers.forEach { installer -> with(installer) { install(navigator) } }
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_KEYS = "navKeys"
        private val KEYS_SERIALIZER = ListSerializer(NavKeySerializer<NavKey>())

        /** NavKeys are @Serializable, so they travel in the Fragment arguments (and survive process death). */
        fun args(keys: List<NavKey>): Bundle {
            require(keys.isNotEmpty()) { "Compose host needs at least one key" }
            return bundleOf(ARG_KEYS to encodeToSavedState(KEYS_SERIALIZER, keys))
        }
    }
}

/**
 * [Navigator] for features hosted inside [ComposeHostFragment]:
 * - screen-to-screen navigation stays in the Nav3 back stack,
 * - "back" on the last Compose screen leaves the host (pops the Fragment),
 * - links go through the app-wide deep link pipeline.
 */
private class HostedNavigator(
    private val backStack: BackStackNavigator,
    private val navController: NavController,
    private val deepLinks: LegacyDeepLinkNavigator,
) : Navigator {

    override fun navigate(key: NavKey) = backStack.navigate(key)

    override fun goBack(): Boolean = backStack.goBack() || navController.popBackStack()

    override fun resetTo(keys: List<NavKey>) = backStack.resetTo(keys)

    override fun openLink(uri: String): OpenLinkResult = deepLinks.openInApp(uri)

    override fun resumePendingLink(): Boolean = deepLinks.resumePending()
}
