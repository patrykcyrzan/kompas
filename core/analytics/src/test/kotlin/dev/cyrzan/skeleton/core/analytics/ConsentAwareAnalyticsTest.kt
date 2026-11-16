package dev.cyrzan.skeleton.core.analytics

import dev.cyrzan.skeleton.core.analytics.consent.ConsentCategory
import dev.cyrzan.skeleton.core.analytics.consent.ConsentRepository
import dev.cyrzan.skeleton.core.analytics.consent.ConsentState
import dev.cyrzan.skeleton.core.analytics.provider.AnalyticsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentAwareAnalyticsTest {

    private val consent = FakeConsentRepository()
    private val firebase = RecordingProvider(ConsentCategory.ANALYTICS)
    private val anonymous = RecordingProvider(requiredConsent = null)
    private val analytics = ConsentAwareAnalytics(
        providers = setOf(firebase, anonymous),
        consentRepository = consent,
        scope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    private val event = AnalyticsEvent(
        name = "search",
        params = listOf(anonymous("screen", "home"), personal("query", "red shoes")),
    )

    @Test
    fun `without consent only the anonymized event leaves the device`() {
        analytics.track(event)

        assertTrue(firebase.events.isEmpty())
        assertEquals(listOf(event.anonymized()), anonymous.events)
        assertEquals(listOf("screen"), anonymous.events.single().params.map { it.key })
    }

    @Test
    fun `with consent the consented provider gets the full event`() {
        consent.decide(setOf(ConsentCategory.ANALYTICS))

        analytics.track(event)

        assertEquals(listOf(event), firebase.events)
        assertEquals(listOf(event.anonymized()), anonymous.events)
    }

    @Test
    fun `user id reaches only consented providers, also when consent comes later`() {
        analytics.identify("user-1")
        assertEquals(emptyList<String?>(), firebase.userIds)

        consent.decide(setOf(ConsentCategory.ANALYTICS))

        assertEquals(listOf<String?>("user-1"), firebase.userIds)
        assertEquals(emptyList<String?>(), anonymous.userIds)
    }

    @Test
    fun `revoking consent stops the provider and resets it`() {
        consent.decide(setOf(ConsentCategory.ANALYTICS))
        consent.decide(emptySet())

        analytics.track(event)

        assertEquals(listOf("granted", "revoked"), firebase.lifecycle)
        assertTrue(firebase.events.isEmpty())
    }

    private class FakeConsentRepository : ConsentRepository {
        override val state = MutableStateFlow(ConsentState.Undecided)
        override val needsDecision get() = state.value.decidedAtMillis == null
        override fun decide(granted: Set<ConsentCategory>) {
            state.value = ConsentState(granted, policyVersion = 1, decidedAtMillis = 0)
        }
    }

    private class RecordingProvider(override val requiredConsent: ConsentCategory?) : AnalyticsProvider {
        val events = mutableListOf<AnalyticsEvent>()
        val userIds = mutableListOf<String?>()
        val lifecycle = mutableListOf<String>()

        override fun onConsentGranted() { lifecycle += "granted" }
        override fun onConsentRevoked() { lifecycle += "revoked" }
        override fun track(event: AnalyticsEvent) { events += event }
        override fun identify(userId: String?) { userIds += userId }
    }
}
