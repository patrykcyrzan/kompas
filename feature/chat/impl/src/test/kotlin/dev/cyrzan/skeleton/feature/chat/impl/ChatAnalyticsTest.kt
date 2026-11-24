package dev.cyrzan.skeleton.feature.chat.impl

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.analytics.anonymous
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAnalyticsTest {

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<AnalyticsEvent>()
        override fun track(event: AnalyticsEvent) {
            events += event
        }
        override fun identify(userId: String?) = Unit
    }

    private val analytics = RecordingAnalytics()
    private val chatAnalytics = ChatAnalytics(analytics)

    @Test
    fun `a click is reported with the screen's own source name`() {
        chatAnalytics.buttonClicked("orders_list")

        assertEquals(
            listOf(AnalyticsEvent("chat_button_clicked", listOf(anonymous("source", "orders_list")))),
            analytics.events,
        )
    }

    @Test
    fun `a click without a source is still reported, without the parameter`() {
        chatAnalytics.buttonClicked(null)

        assertEquals(listOf(AnalyticsEvent("chat_button_clicked")), analytics.events)
    }

    @Test
    fun `the source is anonymous, so it is sent also without consent`() {
        chatAnalytics.buttonClicked("home")

        assertEquals(analytics.events.single(), analytics.events.single().anonymized())
    }
}
