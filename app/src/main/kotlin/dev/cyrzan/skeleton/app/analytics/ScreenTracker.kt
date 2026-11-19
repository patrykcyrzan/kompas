package dev.cyrzan.skeleton.app.analytics

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.analytics.anonymous
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only sender of `screen_viewed`: features just name their entries (`screenName("home")`),
 * navigation reports what is on top and the app reports whether the user can see it.
 *
 * A screen view is counted when a new entry becomes visible, and again when the user returns to it –
 * from the background or from behind the lock. Recomposition and rotation are not views:
 * the tracker is a singleton and remembers which entry it already reported.
 *
 * Called on the main thread only.
 */
@Singleton
internal class ScreenTracker @Inject constructor(
    private val analytics: Analytics,
) {
    private var screen: Screen? = null
    private var visible = false
    private var reportedEntryId: String? = null

    /** The entry on top of navigation changed (also the first one after the Activity is created). */
    fun onScreenChanged(screen: Screen) {
        this.screen = screen
        reportIfNeeded()
    }

    /** `false` while the app is in the background or locked – the user sees no screen then. */
    fun setVisible(visible: Boolean) {
        this.visible = visible
        // Whatever is shown next counts as a new view, also the same entry.
        if (!visible) reportedEntryId = null
        reportIfNeeded()
    }

    private fun reportIfNeeded() {
        val screen = screen ?: return
        if (!visible || screen.entryId == reportedEntryId) return
        reportedEntryId = screen.entryId
        // Unnamed entries are skipped (and logged by navigation) – better no event than a made-up name.
        val name = screen.name ?: return
        analytics.track(AnalyticsEvent(SCREEN_VIEWED, listOf(anonymous("screen", name))))
    }

    private companion object {
        const val SCREEN_VIEWED = "screen_viewed"
    }
}

/** [entryId] tells views apart (see `NavigationState.currentEntryId`), [name] is what gets reported. */
internal data class Screen(val entryId: String, val name: String?)
