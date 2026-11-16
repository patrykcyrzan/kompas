package dev.cyrzan.skeleton.core.analytics

import dev.cyrzan.skeleton.core.analytics.consent.ConsentCategory
import dev.cyrzan.skeleton.core.analytics.consent.ConsentRepository
import dev.cyrzan.skeleton.core.analytics.provider.AnalyticsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Routes events by consent:
 * - providers without [AnalyticsProvider.requiredConsent] always get the anonymized event,
 * - the others get the full event only while their category is granted.
 *
 * Until the first consent emission is applied nothing reaches consented providers – the safe default.
 */
internal class ConsentAwareAnalytics(
    private val providers: Set<AnalyticsProvider>,
    consentRepository: ConsentRepository,
    scope: CoroutineScope,
) : Analytics {

    private val lock = Any()
    private var granted: Set<ConsentCategory> = emptySet()
    private var userId: String? = null

    init {
        scope.launch {
            consentRepository.state
                .map { it.granted }
                .distinctUntilChanged()
                .collect(::applyConsent)
        }
    }

    override fun track(event: AnalyticsEvent) = synchronized(lock) {
        val anonymized by lazy { event.anonymized() }
        providers.forEach { provider ->
            when (provider.requiredConsent) {
                null -> provider.track(anonymized)
                in granted -> provider.track(event)
                else -> Unit
            }
        }
    }

    override fun identify(userId: String?) = synchronized(lock) {
        this.userId = userId
        consentedProviders().forEach { it.identify(userId) }
    }

    private fun applyConsent(newGranted: Set<ConsentCategory>) = synchronized(lock) {
        val previous = granted
        granted = newGranted
        providers.forEach { provider ->
            val required = provider.requiredConsent ?: return@forEach
            val was = required in previous
            val now = required in newGranted
            when {
                !was && now -> {
                    provider.onConsentGranted()
                    userId?.let(provider::identify)
                }
                was && !now -> provider.onConsentRevoked()
            }
        }
    }

    private fun consentedProviders() = providers.filter { it.requiredConsent?.let { c -> c in granted } == true }
}
