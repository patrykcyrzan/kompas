package dev.cyrzan.skeleton.core.analytics.consent

import kotlinx.coroutines.flow.StateFlow

interface ConsentRepository {
    val state: StateFlow<ConsentState>

    /** True before the first decision and after the policy changed – show the consent screen. */
    val needsDecision: Boolean

    /** Replaces the whole decision: categories missing from [granted] are revoked. */
    fun decide(granted: Set<ConsentCategory>)
}
