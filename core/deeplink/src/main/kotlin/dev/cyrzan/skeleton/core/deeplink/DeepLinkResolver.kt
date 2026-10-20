package dev.cyrzan.skeleton.core.deeplink

/**
 * URI -> decision. Pure logic: it never navigates, the platform adapter executes the [DeepLinkResult].
 *
 * Pipeline: parse -> origin -> route -> back stack policy.
 */
class DeepLinkResolver(
    private val origins: List<DeepLinkOrigin>,
    routes: Collection<DeepLinkRoute>,
    private val backStackPolicy: BackStackPolicy,
) {
    private val routes: List<DeepLinkRoute> = routes.sortedWith(DeepLinkRoute.SPECIFICITY)

    init {
        require(origins.isNotEmpty()) { "At least one DeepLinkOrigin is required" }
        val ambiguous = routes.groupBy { it.shape }.values.filter { it.size > 1 }
        require(ambiguous.isEmpty()) {
            "Ambiguous deep link routes: " + ambiguous.joinToString { group -> group.joinToString(" vs ") { it.pattern } }
        }
    }

    fun resolve(uri: String): DeepLinkResult {
        val link = DeepLink.parse(uri) ?: return DeepLinkResult.Invalid("Malformed URI")

        val pathSegments = origins.firstNotNullOfOrNull { it.pathSegmentsOf(link) }
            ?: return DeepLinkResult.NotMatched

        val destination = routes.firstNotNullOfOrNull { it.resolve(pathSegments, link.queryParameters) }
            ?: return DeepLinkResult.NotMatched

        return DeepLinkResult.Navigate(destination, backStackPolicy.backStackFor(destination))
    }
}

/**
 * Results deliberately don't carry the URI – it may contain secrets and results tend to end up in logs.
 *
 * [Navigate] carries both the target and a synthetic back stack: the platform adapter decides which one to use.
 */
sealed interface DeepLinkResult {
    data class Navigate(val destination: Destination, val backStack: List<Destination>) : DeepLinkResult

    /** Valid link, but not ours (unknown origin) or no route matches. */
    data object NotMatched : DeepLinkResult

    /** Malformed input, rejected before matching. */
    data class Invalid(val reason: String) : DeepLinkResult
}
