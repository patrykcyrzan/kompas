package dev.cyrzan.skeleton.app.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkOrigin
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.RootedBackStackPolicy
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey

/**
 * App-wide deep link configuration. Must stay in sync with the intent filters in AndroidManifest.xml.
 *
 * TODO: replace the placeholder host with a domain you own (and host assetlinks.json for App Links).
 */
internal val DEEP_LINK_ORIGINS = listOf(
    DeepLinkOrigin.Web(host = "kompas.example.com"),
    DeepLinkOrigin.CustomScheme(scheme = "kompas"),
)

internal fun createDeepLinkResolver(
    routes: Collection<DeepLinkRoute>,
    guards: Collection<DeepLinkGuard> = emptyList(),
) = DeepLinkResolver(
    origins = DEEP_LINK_ORIGINS,
    routes = routes,
    backStackPolicy = RootedBackStackPolicy(root = HomeNavKey),
    guards = guards,
)
