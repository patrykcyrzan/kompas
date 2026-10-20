package dev.cyrzan.skeleton.core.deeplink

/**
 * URI -> decision. Pure logic: it never navigates, the platform adapter executes the [DeepLinkResult].
 *
 * Pipeline: parse -> origin -> route -> guards (by priority) -> back stack policy.
 */
class DeepLinkResolver(
    private val origins: List<DeepLinkOrigin>,
    routes: Collection<DeepLinkRoute>,
    private val backStackPolicy: BackStackPolicy,
    guards: Collection<DeepLinkGuard> = emptyList(),
) {
    private val routes: List<DeepLinkRoute> = routes.sortedWith(DeepLinkRoute.SPECIFICITY)

    // Hilt sets have no defined order – sort explicitly so evaluation is deterministic.
    private val guards: List<DeepLinkGuard> =
        guards.sortedWith(compareBy<DeepLinkGuard>({ it.priority }, { it::class.qualifiedName.orEmpty() }))

    init {
        require(origins.isNotEmpty()) { "At least one DeepLinkOrigin is required" }
        val ambiguous = routes.groupBy { it.shape }.values.filter { it.size > 1 }
        require(ambiguous.isEmpty()) {
            "Ambiguous deep link routes: " + ambiguous.joinToString { group -> group.joinToString(" vs ") { it.pattern } }
        }
    }

    fun resolve(uri: String, source: DeepLinkSource): DeepLinkResult {
        val link = DeepLink.parse(uri) ?: return DeepLinkResult.Invalid("Malformed URI")

        val (origin, pathSegments) = origins.firstNotNullOfOrNull { origin ->
            origin.pathSegmentsOf(link)?.let { origin to it }
        } ?: return DeepLinkResult.NotMatched

        val destination = routes.firstNotNullOfOrNull { it.resolve(pathSegments, link.queryParameters) }
            ?: return DeepLinkResult.NotMatched

        val request = DeepLinkRequest(destination, source, origin)
        guards.forEach { guard ->
            when (val decision = guard.check(request)) {
                DeepLinkGuard.Decision.Allow -> Unit
                is DeepLinkGuard.Decision.Reject -> return DeepLinkResult.Rejected(destination, decision.reason)
                is DeepLinkGuard.Decision.Redirect -> return DeepLinkResult.Redirect(
                    destination = decision.destination,
                    backStack = backStackPolicy.backStackFor(decision.destination),
                    resumable = decision.resumeAfter,
                )
            }
        }
        return DeepLinkResult.Navigate(destination, backStackPolicy.backStackFor(destination))
    }
}

/**
 * Results deliberately don't carry the URI – it may contain secrets and results tend to end up in logs.
 *
 * [Navigate] and [Redirect] carry both the target and a synthetic back stack: the platform adapter
 * uses the back stack for links from outside the app and just pushes the target for in-app links.
 */
sealed interface DeepLinkResult {
    data class Navigate(val destination: Destination, val backStack: List<Destination>) : DeepLinkResult

    data class Redirect(
        val destination: Destination,
        val backStack: List<Destination>,
        val resumable: Boolean,
    ) : DeepLinkResult

    data class Rejected(val destination: Destination, val reason: String) : DeepLinkResult

    /** Valid link, but not ours (unknown origin) or no route matches. */
    data object NotMatched : DeepLinkResult

    /** Malformed input, rejected before matching. */
    data class Invalid(val reason: String) : DeepLinkResult
}
