package dev.cyrzan.skeleton.feature.chat.impl

import dev.cyrzan.skeleton.core.analytics.Analytics
import dev.cyrzan.skeleton.core.analytics.AnalyticsEvent
import dev.cyrzan.skeleton.core.analytics.anonymous
import javax.inject.Inject

/** The chat's analytics events, named and classified in one place. */
internal class ChatAnalytics @Inject constructor(
    private val analytics: Analytics,
) {

    /**
     * [source] comes from the screen embedding the button – a fixed name, so it may be sent without consent.
     * Screens that give none are still counted, just without the parameter.
     */
    fun buttonClicked(source: String?) {
        analytics.track(AnalyticsEvent(CHAT_BUTTON_CLICKED, listOfNotNull(source?.let { anonymous("source", it) })))
    }

    private companion object {
        const val CHAT_BUTTON_CLICKED = "chat_button_clicked"
    }
}
