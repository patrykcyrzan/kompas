package dev.cyrzan.skeleton.core.deeplink

/**
 * Deep links are untrusted external input. Guards decide whether a resolved destination
 * may actually be opened (auth, app lock, kill switch, allow-listing...).
 *
 * Guards run in ascending [priority] order; the first decision other than [Decision.Allow] wins.
 */
fun interface DeepLinkGuard {

    fun check(request: DeepLinkRequest): Decision

    /** Lower runs first. Use the constants from [GuardPriority]. */
    val priority: Int get() = GuardPriority.DEFAULT

    sealed interface Decision {
        data object Allow : Decision

        /** Don't open anything. The user stays where they are. */
        data class Reject(val reason: String) : Decision

        /**
         * Open [destination] instead (e.g. login). With [resumeAfter] the original link is kept
         * (in memory, for a limited time) and can be resumed once – it is then resolved again,
         * so every guard re-checks it against the new state.
         */
        data class Redirect(val destination: Destination, val resumeAfter: Boolean = true) : Decision
    }
}

/** What a guard gets to decide on. */
data class DeepLinkRequest(
    val destination: Destination,
    val source: DeepLinkSource,
    val origin: DeepLinkOrigin,
)

/** Evaluation order: cheap, global "hard stops" first, per-destination rules later. */
object GuardPriority {
    const val KILL_SWITCH = 0
    const val DEBUG = 100
    const val APP_LOCK = 200
    const val ONBOARDING = 300
    const val AUTHENTICATION = 400
    const val SOURCE = 500
    const val ARGUMENTS = 600
    const val DEFAULT = 1000
}
