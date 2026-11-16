package dev.cyrzan.skeleton.core.analytics.provider

import android.util.Log
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import javax.inject.Inject

/**
 * Stub of the consent-free tier: counts what happens, never who did it.
 *
 * A real implementation sends to our own backend (or PostHog without persistence) with:
 * no installation/advertising/user id, a new random id per event at most, no local storage
 * that outlives the batch – and the server truncates IPs and only keeps aggregates.
 * What is "anonymous enough" is a legal call – agree it with the DPO.
 */
internal class AnonymousMetricsProvider @Inject constructor() : AnalyticsProvider {

    override val requiredConsent = null

    override fun track(event: AnalyticsEvent) {
        Log.d(TAG, "count ${event.name} ${event.params.associate { it.key to it.value }}")
    }

    private companion object {
        const val TAG = "Analytics/Anonymous"
    }
}
