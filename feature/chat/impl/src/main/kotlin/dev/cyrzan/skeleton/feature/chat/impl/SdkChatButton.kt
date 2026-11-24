package dev.cyrzan.skeleton.feature.chat.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.core.navigation.LocalNavigator
import dev.cyrzan.skeleton.feature.chat.api.ChatButton
import dev.cyrzan.skeleton.feature.chat.api.ChatNavKey
import dev.cyrzan.skeleton.feature.chat.impl.sdk.FakeChatSdk
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The real [ChatButton]: a draggable FAB with the chat icon and the SDK's unread count, opens the conversation.
 * Shown when [ChatVisibility] says so.
 */
internal class SdkChatButton @Inject constructor(
    private val chatAnalytics: ChatAnalytics,
    private val position: ChatButtonPosition,
    private val visibility: ChatVisibility,
) : ChatButton {

    @Composable
    override fun Overlay(source: String?, modifier: Modifier, bottomPadding: Dp) {
        // Hidden by default; ChatVisibility (our rules, not the SDK) decides when it shows.
        val visible by visibility.isVisible.collectAsStateWithLifecycle()
        AnimatedVisibility(visible = visible, modifier = modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
            DraggableButton(source, bottomPadding)
        }
    }

    @Composable
    private fun DraggableButton(source: String?, bottomPadding: Dp) {
        // No pointer input (clickable, pointerInput, draggable...) on the overlay itself: a layer without it
        // is transparent to touches, so only the button takes them and the screen below stays usable.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = EDGE_MARGIN, top = EDGE_MARGIN, end = EDGE_MARGIN, bottom = bottomPadding),
        ) {
            var buttonSize by remember { mutableStateOf(IntSize.Zero) }
            // The range of the button's top-left corner.
            val maxX = (constraints.maxWidth - buttonSize.width).coerceAtLeast(0).toFloat()
            val maxY = (constraints.maxHeight - buttonSize.height).coerceAtLeast(0).toFloat()
            // Pixels only while dragging; otherwise the shared fraction decides.
            var dragged by remember { mutableStateOf<Offset?>(null) }

            ChatFab(
                source = source,
                modifier = Modifier
                    .onSizeChanged { buttonSize = it }
                    // Before pointerInput: the touch target moves with the button.
                    .offset {
                        val at = dragged ?: Offset(position.fraction.x * maxX, position.fraction.y * maxY)
                        IntOffset(at.x.roundToInt(), at.y.roundToInt())
                    }
                    .pointerInput(maxX, maxY) {
                        // A drag starts only past the touch slop – a tap still reaches the FAB's onClick.
                        detectDragGestures(
                            onDragStart = { dragged = Offset(position.fraction.x * maxX, position.fraction.y * maxY) },
                            onDrag = { change, amount ->
                                change.consume()
                                val from = dragged ?: return@detectDragGestures
                                dragged = Offset((from.x + amount.x).coerceIn(0f, maxX), (from.y + amount.y).coerceIn(0f, maxY))
                            },
                            onDragEnd = {
                                dragged?.let { at ->
                                    position.fraction = Offset(
                                        if (maxX > 0f) at.x / maxX else position.fraction.x,
                                        if (maxY > 0f) at.y / maxY else position.fraction.y,
                                    )
                                }
                                dragged = null
                            },
                            onDragCancel = { dragged = null },
                        )
                    },
            )
        }
    }

    @Composable
    private fun ChatFab(source: String?, modifier: Modifier) {
        val navigator = LocalNavigator.current
        val unread by FakeChatSdk.unreadCount.collectAsStateWithLifecycle()
        FloatingActionButton(
            onClick = {
                chatAnalytics.buttonClicked(source)
                navigator.navigate(ChatNavKey)
            },
            modifier = modifier,
        ) {
            BadgedBox(
                badge = {
                    if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) }
                },
            ) {
                Icon(ChatIcon, contentDescription = if (unread > 0) "Chat, $unread unread" else "Chat")
            }
        }
    }

    private companion object {
        val EDGE_MARGIN = 16.dp
    }
}
