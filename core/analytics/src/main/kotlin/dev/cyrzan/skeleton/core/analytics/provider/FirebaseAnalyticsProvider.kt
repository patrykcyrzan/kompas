package dev.cyrzan.skeleton.core.analytics.provider

import android.util.Log
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.analytics.consent.ConsentCategory
import javax.inject.Inject

/**
 * Stub – logs what would be sent. The real one wraps `FirebaseAnalytics` and needs in the manifest:
 * `firebase_analytics_collection_enabled=false`, `google_analytics_default_allow_analytics_storage=false`
 * and `google_analytics_default_allow_ad_storage=false`, so nothing is collected before [onConsentGranted].
 */
internal class FirebaseAnalyticsProvider @Inject constructor() : AnalyticsProvider {

    override val requiredConsent = ConsentCategory.ANALYTICS

    override fun onConsentGranted() {
        // firebase.setConsent(mapOf(ANALYTICS_STORAGE to GRANTED)); firebase.setAnalyticsCollectionEnabled(true)
        Log.d(TAG, "enabled")
    }

    override fun onConsentRevoked() {
        // firebase.setAnalyticsCollectionEnabled(false); firebase.resetAnalyticsData()
        Log.d(TAG, "disabled, data reset")
    }

    override fun track(event: AnalyticsEvent) {
        // firebase.logEvent(event.name) { event.params.forEach { param(it.key, ...) } }
        Log.d(TAG, "logEvent ${event.name} ${event.params.associate { it.key to it.value }}")
    }

    override fun identify(userId: String?) {
        // firebase.setUserId(userId)
        Log.d(TAG, "setUserId $userId")
    }

    private companion object {
        const val TAG = "Analytics/Firebase"
    }
}
