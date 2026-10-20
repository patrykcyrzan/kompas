package dev.cyrzan.skeleton.core.deeplink

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Holds the link that caused a [DeepLinkResult.Redirect] (e.g. to login) so it can be resumed afterwards.
 *
 * Security properties:
 * - in memory only – never persisted, gone with the process,
 * - single use – [consume] clears it,
 * - expires after [ttl] – an old link can't be "resumed" by an unrelated login an hour later,
 * - stores the raw URI, not the result – resuming resolves it again, so guards re-check it.
 *
 * Not thread-safe: use from the main thread.
 */
class PendingDeepLinkStore(
    private val ttl: Duration = 5.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var pending: Entry? = null

    fun save(uri: String, source: DeepLinkSource) {
        pending = Entry(PendingDeepLink(uri, source), timeSource.markNow())
    }

    fun consume(): PendingDeepLink? {
        val entry = pending ?: return null
        pending = null
        return entry.link.takeIf { entry.savedAt.elapsedNow() <= ttl }
    }

    fun clear() {
        pending = null
    }

    private class Entry(val link: PendingDeepLink, val savedAt: TimeMark)
}

data class PendingDeepLink(val uri: String, val source: DeepLinkSource)
