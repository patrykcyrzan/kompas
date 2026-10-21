package dev.cyrzan.skeleton.app.deeplink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.app.R
import dev.cyrzan.skeleton.app.compose.ComposeHostFragment
import dev.cyrzan.skeleton.app.legacy.LoginActivity
import dev.cyrzan.skeleton.app.legacy.OrderDetailsFragment
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult

/**
 * Executes deep link results on the legacy [NavController] – the counterpart of the Nav3 `AppNavigator`.
 *
 * The resolver (and [DeepLinkDispatcher]) don't know anything about Fragments: they return
 * [Destination]s. This class is the only place that knows how a destination is shown in the
 * legacy UI: as a Fragment from nav_graph.xml, as an Activity, or as Compose screens inside
 * [ComposeHostFragment].
 *
 * @param startActivity launches Activity destinations (e.g. login) – the host decides how,
 *   so it can observe the result and resume the pending link.
 */
internal class LegacyDeepLinkNavigator(
    private val context: Context,
    private val navController: NavController,
    private val dispatcher: DeepLinkDispatcher,
    private val startActivity: (Intent) -> Unit,
) {

    /** Link delivered by the OS (cold start or `onNewIntent`). */
    fun openExternal(uri: String) {
        apply(dispatcher.dispatch(uri, DeepLinkSource.External), DeepLinkSource.External)
    }

    /** Link from inside the app, e.g. received from the backend. */
    fun openInApp(uri: String): OpenLinkResult =
        apply(dispatcher.dispatch(uri, DeepLinkSource.InApp), DeepLinkSource.InApp)

    /** Resumes the link interrupted by a redirect (e.g. after a successful login). */
    fun resumePending(): Boolean {
        val (source, result) = dispatcher.resumePending() ?: return false
        return apply(result, source) == OpenLinkResult.Opened
    }

    private fun apply(result: DeepLinkResult, source: DeepLinkSource): OpenLinkResult = when (result) {
        is DeepLinkResult.Navigate -> show(result.destination, result.backStack, source)
        is DeepLinkResult.Redirect -> show(result.destination, result.backStack, source)
        is DeepLinkResult.Rejected -> OpenLinkResult.Blocked
        is DeepLinkResult.Invalid, DeepLinkResult.NotMatched -> OpenLinkResult.Unsupported
    }

    /**
     * External link: rebuild the whole stack (root first), like `resetTo()` in the Nav3 version.
     * In-app link: push only the destination on top of the current screen.
     */
    private fun show(destination: Destination, backStack: List<Destination>, source: DeepLinkSource): OpenLinkResult {
        val destinations = if (source == DeepLinkSource.External) backStack else listOf(destination)
        // Map everything first: a link we can't fully show must not leave a half-built back stack.
        val targets = destinations.map { it.toTarget() ?: return OpenLinkResult.Unsupported }.mergeCompose()

        targets.forEachIndexed { index, target ->
            when (target) {
                is Target.Screen -> {
                    val options = if (index == 0 && source == DeepLinkSource.External) {
                        navOptions { popUpTo(navController.graph.id) { inclusive = true } }
                    } else {
                        null
                    }
                    navController.navigate(target.id, target.args, options)
                }
                is Target.Compose -> navController.navigate(R.id.composeHostFragment, ComposeHostFragment.args(target.keys))
                is Target.Activity -> startActivity(target.intent)
            }
        }
        return OpenLinkResult.Opened
    }

    private sealed interface Target {
        /** A Fragment destination from nav_graph.xml. */
        data class Screen(@IdRes val id: Int, val args: Bundle? = null) : Target

        /** New Compose screens – shown together in one [ComposeHostFragment]. */
        data class Compose(val keys: List<NavKey>) : Target

        data class Activity(val intent: Intent) : Target
    }

    /** The single mapping table. Migrating a screen = changing (then deleting) its line here. */
    private fun Destination.toTarget(): Target? = when (this) {
        LegacyDestination.Home -> Target.Screen(R.id.homeFragment)
        is LegacyDestination.OrderDetails -> Target.Screen(R.id.orderDetailsFragment, OrderDetailsFragment.args(orderId))
        LegacyDestination.Profile -> Target.Screen(R.id.profileFragment)
        LegacyDestination.Login -> Target.Activity(Intent(context, LoginActivity::class.java))
        // Feature modules' keys are both Destination and NavKey – they are Compose screens.
        is NavKey -> Target.Compose(listOf(this))
        else -> null
    }

    /** Consecutive Compose screens share one host, so "back" between them stays inside Compose. */
    private fun List<Target>.mergeCompose(): List<Target> = fold(mutableListOf()) { acc, target ->
        val last = acc.lastOrNull()
        if (target is Target.Compose && last is Target.Compose) {
            acc[acc.lastIndex] = Target.Compose(last.keys + target.keys)
        } else {
            acc += target
        }
        acc
    }
}
