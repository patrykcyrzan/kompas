package dev.cyrzan.skeleton.app.analytics

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTrackerTest {

    private class RecordingAnalytics : Analytics {
        val screens = mutableListOf<Any>()
        override fun track(event: AnalyticsEvent) {
            if (event.name == "screen_viewed") screens += event.params.single { it.key == "screen" }.value
        }
        override fun identify(userId: String?) = Unit
    }

    private val analytics = RecordingAnalytics()
    private val tracker = ScreenTracker(analytics).apply { setVisible(true) }

    private val home = Screen("tab0/0/home", "home")
    private val details7 = Screen("tab0/1/details(7)", "details")
    private val details8 = Screen("tab0/2/details(8)", "details")

    @Test
    fun `every new entry is a view, also of the same screen with other arguments`() {
        tracker.onScreenChanged(home)
        tracker.onScreenChanged(details7)
        tracker.onScreenChanged(details8)
        tracker.onScreenChanged(details7)
        tracker.onScreenChanged(home)

        assertEquals(listOf("home", "details", "details", "details", "home"), analytics.screens)
    }

    @Test
    fun `the same entry reported again (recomposition, rotation) is not a view`() {
        tracker.onScreenChanged(home)
        tracker.onScreenChanged(home)

        assertEquals(listOf("home"), analytics.screens)
    }

    @Test
    fun `returning from the background or the lock is a view of the same screen`() {
        tracker.onScreenChanged(home)
        tracker.setVisible(false)
        tracker.setVisible(true)

        assertEquals(listOf("home", "home"), analytics.screens)
    }

    @Test
    fun `nothing is reported while the user cannot see it, only what is on top once they can`() {
        tracker.setVisible(false)
        tracker.onScreenChanged(home)
        tracker.onScreenChanged(details7)

        assertEquals(emptyList<Any>(), analytics.screens)
        tracker.setVisible(true)
        assertEquals(listOf("details"), analytics.screens)
    }

    @Test
    fun `unnamed entries are not reported`() {
        tracker.onScreenChanged(Screen("tab0/1/x", name = null))

        assertEquals(emptyList<Any>(), analytics.screens)
    }
}
