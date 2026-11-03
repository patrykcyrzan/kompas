package dev.cyrzan.skeleton.core.deeplink

import dev.cyrzan.skeleton.core.deeplink.DeepLinkGuard.Decision

/** Rejects [DebugOnly] destinations unless [isDebugBuild]. */
class DebugOnlyGuard(private val isDebugBuild: Boolean) : DeepLinkGuard {
    override val priority = GuardPriority.DEBUG

    override fun check(request: DeepLinkRequest): Decision =
        if (request.destination is DebugOnly && !isDebugBuild) Decision.Reject("Debug-only destination") else Decision.Allow
}

/** Rejects [InAppOnly] destinations requested from outside the app. */
class InAppOnlyGuard : DeepLinkGuard {
    override val priority = GuardPriority.SOURCE

    override fun check(request: DeepLinkRequest): Decision =
        if (request.destination is InAppOnly && request.source != DeepLinkSource.InApp) {
            Decision.Reject("Destination can be opened only from within the app")
        } else {
            Decision.Allow
        }
}

/** Redirects to [loginDestination] for [RequiresAuthentication] destinations when there is no session. */
class AuthenticationGuard(
    private val isAuthenticated: () -> Boolean,
    private val loginDestination: Destination,
) : DeepLinkGuard {
    override val priority = GuardPriority.AUTHENTICATION

    override fun check(request: DeepLinkRequest): Decision =
        if (request.destination is RequiresAuthentication && !isAuthenticated()) {
            Decision.Redirect(loginDestination, resumeAfter = true)
        } else {
            Decision.Allow
        }
}
