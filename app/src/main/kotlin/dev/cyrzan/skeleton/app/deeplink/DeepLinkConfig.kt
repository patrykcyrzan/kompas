package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkOrigin
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.deeplink.ParentBackStackPolicy
import dev.cyrzan.skeleton.feature.auth.api.LoginNavKey
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrderDetailsNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.profile.api.AboutNavKey
import dev.cyrzan.skeleton.feature.profile.api.ProfileNavKey
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey

/**
 * App-wide deep link configuration. Must stay in sync with the intent filters in AndroidManifest.xml.
 *
 * TODO: replace the placeholder host with a domain you own (and host assetlinks.json for App Links).
 */
internal val DEEP_LINK_ORIGINS = listOf(
    DeepLinkOrigin.Web(host = "kompas.example.com"),
    DeepLinkOrigin.CustomScheme(scheme = "kompas"),
)

/**
 * Where a screen opened from outside the app lives: walking up the parents must end at a tab root
 * (see TopLevelTab), which tells the navigator which tab to select – or at a full-screen screen
 * (see isFullScreen), which opens above the tabs.
 *
 * Only used for links from outside the app. In-app navigation pushes on the current tab instead,
 * so e.g. item details opened from an order stay on the Orders tab.
 */
internal fun parentOf(destination: Destination): Destination? = when (destination) {
    HomeNavKey, OrdersNavKey, ProfileNavKey -> null // tab roots
    LoginNavKey -> null // full screen: opens above whatever tab is selected
    is DetailsNavKey -> HomeNavKey
    is OrderDetailsNavKey -> OrdersNavKey
    SettingsNavKey, AboutNavKey -> ProfileNavKey
    else -> HomeNavKey
}

internal fun createDeepLinkResolver(
    routes: Collection<DeepLinkRoute>,
    guards: Collection<DeepLinkGuard> = emptyList(),
) = DeepLinkResolver(
    origins = DEEP_LINK_ORIGINS,
    routes = routes,
    backStackPolicy = ParentBackStackPolicy(::parentOf),
    guards = guards,
)
