package dev.cyrzan.skeleton.app.analytics

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The only caller of [Analytics.identify]: who the user is follows the session, so sign-in, sign-out
 * and a session restored on cold start are all covered without features having to remember it.
 * Lives in `:app` – the one module that knows both the session and analytics.
 *
 * Consent is not checked here: the router keeps the id and passes it on only to consented providers.
 */
internal class AnalyticsIdentity @Inject constructor(
    private val session: SessionRepository,
    private val analytics: Analytics,
) {
    fun start(scope: CoroutineScope) {
        // A StateFlow never repeats a value, so the same user is never identified twice in a row.
        scope.launch { session.userId.collect(analytics::identify) }
    }
}
