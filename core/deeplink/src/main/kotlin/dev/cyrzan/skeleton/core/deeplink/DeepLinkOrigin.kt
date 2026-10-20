package dev.cyrzan.skeleton.core.deeplink

/**
 * Where deep links are accepted from. Configured once by the app, so features declare
 * only paths and never know the domain or the custom scheme.
 */
sealed interface DeepLinkOrigin {

    /** Path segments relevant for route matching if [link] comes from this origin, `null` otherwise. */
    fun pathSegmentsOf(link: DeepLink): List<String>?

    /** `https://<host>/items/42` -> `[items, 42]` */
    data class Web(
        val host: String,
        val schemes: Set<String> = setOf("https"),
    ) : DeepLinkOrigin {
        override fun pathSegmentsOf(link: DeepLink): List<String>? =
            link.pathSegments.takeIf { link.scheme in schemes && link.host.equals(host, ignoreCase = true) }
    }

    /** `<scheme>://items/42` -> `[items, 42]` (the "host" is treated as the first path segment). */
    data class CustomScheme(val scheme: String) : DeepLinkOrigin {
        override fun pathSegmentsOf(link: DeepLink): List<String>? =
            (listOfNotNull(link.host) + link.pathSegments).takeIf { link.scheme == scheme.lowercase() }
    }
}
