package dev.cyrzan.skeleton.core.analytics

/**
 * The only analytics API features see. Consent and SDKs are hidden behind it:
 * features just describe what happened and classify each parameter (see [Param]).
 */
interface Analytics {
    fun track(event: AnalyticsEvent)

    /** Attached only by providers the user consented to; `null` on sign-out. */
    fun identify(userId: String?)
}
