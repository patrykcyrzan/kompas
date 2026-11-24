package dev.cyrzan.skeleton.feature.chat.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The chat entry point other features put on their screens: a draggable button in a layer above the content.
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     Scaffold(...) { ... }
 *     LocalChatButton.current.Overlay(source = "orders_list")
 * }
 * ```
 *
 * The chat feature owns everything about it – the icon, the unread badge, where it sits, whether it shows
 * at all and where a tap leads – so the screens embedding it never change when the chat does.
 */
interface ChatButton {

    /**
     * Fills its parent. Only the button takes touches – everything under the rest of the overlay stays
     * clickable. The user can drag the button anywhere in the overlay; it keeps that place on every screen.
     *
     * @param source tells analytics where the button was tapped (`chat_button_clicked`). A fixed, anonymous
     *   name chosen by the embedding screen – not necessarily its `screenName`; never ids or user data.
     *   Only for tracking: without it the click is reported without a source.
     * @param bottomPadding how far above the bottom of the overlay (and the system bars / keyboard) the button
     *   may go – e.g. to stay above the screen's own bottom buttons.
     */
    @Composable
    fun Overlay(
        source: String? = null,
        modifier: Modifier = Modifier,
        bottomPadding: Dp = 16.dp,
    )
}

/**
 * Provided by the app with the real button. Without a provider (previews, other features' sandboxes,
 * which do not have the chat on their classpath) nothing is shown.
 */
val LocalChatButton = staticCompositionLocalOf<ChatButton> { NoChatButton }

private object NoChatButton : ChatButton {

    @Composable
    override fun Overlay(source: String?, modifier: Modifier, bottomPadding: Dp) = Unit
}
