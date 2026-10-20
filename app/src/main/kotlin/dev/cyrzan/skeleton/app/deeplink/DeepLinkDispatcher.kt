package dev.cyrzan.skeleton.app.deeplink

import android.content.Intent
import android.util.Log
import androidx.navigation3.runtime.NavKey
import dev.cyrzan.skeleton.app.BuildConfig
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResult
import dev.cyrzan.skeleton.core.deeplink.DeepLinkSource
import dev.cyrzan.skeleton.core.deeplink.Destination
import dev.cyrzan.skeleton.core.deeplink.PendingDeepLinkStore
import dev.cyrzan.skeleton.core.deeplink.redactDeepLink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android side of deep links: resolves, logs safely and remembers links interrupted by a redirect.
 * All decisions are made by the platform-neutral [DeepLinkResolver].
 */
@Singleton
internal class DeepLinkDispatcher @Inject constructor(
    private val resolver: DeepLinkResolver,
    private val pendingLinks: PendingDeepLinkStore,
) {

    fun dispatch(uri: String, source: DeepLinkSource): DeepLinkResult {
        val result = resolver.resolve(uri, source)
        log(uri, source, result)
        if (result is DeepLinkResult.Redirect && result.resumable) {
            pendingLinks.save(uri, source)
        }
        return result
    }

    /**
     * Only `ACTION_VIEW` + data URI is accepted. Intent extras are deliberately ignored:
     * they are arbitrary, attacker-controlled data.
     */
    fun uriOf(intent: Intent?): String? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString

    /** Initial back stack for a cold start from [intent], or `null` when it isn't an openable link. */
    fun startBackStackFor(intent: Intent?): List<NavKey>? {
        val uri = uriOf(intent) ?: return null
        return when (val result = dispatch(uri, DeepLinkSource.External)) {
            is DeepLinkResult.Navigate -> result.backStack.map(Destination::toNavKey)
            is DeepLinkResult.Redirect -> result.backStack.map(Destination::toNavKey)
            else -> null
        }
    }

    private fun log(uri: String, source: DeepLinkSource, result: DeepLinkResult) {
        // Links may carry tokens / personal data: full URI only in debug builds.
        val safeUri = if (BuildConfig.DEBUG) uri else redactDeepLink(uri)
        when (result) {
            is DeepLinkResult.Navigate -> if (BuildConfig.DEBUG) Log.d(TAG, "Opening ($source): $safeUri")
            is DeepLinkResult.Redirect -> Log.i(TAG, "Redirected to ${result.destination.name} ($source): $safeUri")
            is DeepLinkResult.Rejected -> Log.w(TAG, "Rejected ${result.destination.name}: ${result.reason} ($source): $safeUri")
            is DeepLinkResult.Invalid -> Log.w(TAG, "Invalid link: ${result.reason} ($source): $safeUri")
            DeepLinkResult.NotMatched -> Log.w(TAG, "Unsupported link ($source): $safeUri")
        }
    }

    private val Destination.name: String get() = this::class.simpleName.orEmpty()

    private companion object {
        const val TAG = "DeepLink"
    }
}

internal fun Destination.toNavKey(): NavKey = checkNotNull(this as? NavKey) {
    "${this::class.qualifiedName} must implement NavKey to be shown on Android"
}
