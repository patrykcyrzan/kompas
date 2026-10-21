package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.deeplink.RequiresAuthentication
import dev.cyrzan.skeleton.core.deeplink.deepLinkRoute

/**
 * Screens that still live in the monolith (Fragments / Activities in :app).
 *
 * They have no :api module yet, so their destinations and routes are declared here, in one place.
 * When a screen gets extracted into a feature module, its destination becomes a NavKey in that
 * feature's :api and its route moves there – with the same pattern, so public URLs don't change.
 */
internal sealed interface LegacyDestination : Destination {
    data object Home : LegacyDestination
    data class OrderDetails(val orderId: Long) : LegacyDestination
    data object Profile : LegacyDestination, RequiresAuthentication
    data object Login : LegacyDestination
}

internal object LegacyDeepLinks {
    /** `kompas://home` */
    val home: DeepLinkRoute = deepLinkRoute("home") { LegacyDestination.Home }

    /** `kompas://orders/42` */
    val orderDetails: DeepLinkRoute = deepLinkRoute("orders/{orderId}") { params ->
        params.pathLong("orderId")?.let(LegacyDestination::OrderDetails)
    }

    /** `kompas://profile` – requires a session, otherwise redirects to login and resumes afterwards. */
    val profile: DeepLinkRoute = deepLinkRoute("profile") { LegacyDestination.Profile }

    val all: Set<DeepLinkRoute> = setOf(home, orderDetails, profile)
}
