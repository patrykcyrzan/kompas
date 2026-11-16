package dev.cyrzan.skeleton.core.analytics.provider

import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.analytics.consent.ConsentCategory

/**
 * One SDK or backend. Contributed with `@Binds @IntoSet`; never called by features directly –
 * only by the router, which decides what reaches it.
 */
interface AnalyticsProvider {
    /**
     * `null` – needs no consent: gets every event, but only [AnalyticsEvent.anonymized] and never a user id.
     * Otherwise gets full events and the user id, but only while the category is granted.
     */
    val requiredConsent: ConsentCategory?

    /** Start the SDK here, not in `Application.onCreate` – many SDKs send data on init. */
    fun onConsentGranted() = Unit

    /** Stop collecting, drop queued events, forget identifiers. */
    fun onConsentRevoked() = Unit

    fun track(event: AnalyticsEvent)

    fun identify(userId: String?) = Unit
}
