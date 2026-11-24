package dev.cyrzan.skeleton.feature.chat.impl.sdk

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Stands in for a vendor chat SDK (e.g. Zowie): a static entry point initialized from the Application,
 * its own state and its own conversation UI. Treat it as a third-party library – nothing outside
 * `:feature:chat:impl` may touch it, the rest of the app only sees `:feature:chat:api`.
 *
 * Like a real one it needs a configuration (channel...) besides `initialize()`; without one its conversation
 * cannot connect. Whether and when the chat is *offered* is not the SDK's business – see ChatVisibility.
 *
 * Support "answers" a moment after each message; when the conversation is not on screen by then,
 * the answer counts as unread – that is what the badge on the chat button shows.
 */
internal object FakeChatSdk {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var initialized = false

    /** How many conversation views are on screen (more than one during a transition). */
    private var visibleConversations = 0

    private var config: ChatSdkConfig? = null

    /** `false` until [configure] got a configuration – and again after it got `null`. */
    private val connected = MutableStateFlow(false)

    private val messages = MutableStateFlow(emptyList<ChatMessage>())

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    @Suppress("UNUSED_PARAMETER") // A real SDK keeps the context.
    fun initialize(context: Context) {
        initialized = true
    }

    /** Applies the configuration from the host; `null` switches the chat off. Can be called any time, repeatedly. */
    fun configure(config: ChatSdkConfig?) {
        check(initialized) { "FakeChatSdk.initialize() was not called" }
        if (config == this.config) return
        val previous = this.config
        this.config = config
        connected.value = config != null
        // The greeting once, on the first connection – it is what makes the badge show "1".
        if (config != null && previous == null && messages.value.isEmpty()) {
            receive(config.welcomeMessage ?: "Hi! How can we help? (channel ${config.channelId})")
        }
    }

    /** The SDK's own UI – like a vendor's chat Fragment, the host only decides where it goes. */
    @Composable
    fun Conversation(modifier: Modifier = Modifier) {
        check(initialized) { "FakeChatSdk.initialize() was not called" }
        DisposableEffect(Unit) {
            visibleConversations++
            _unreadCount.value = 0
            onDispose { visibleConversations-- }
        }

        val isConnected by connected.collectAsStateWithLifecycle()
        if (!isConnected) {
            // Switched off while open (a new config): the host's back still works.
            Box(modifier, contentAlignment = Alignment.Center) { Text("The chat is not available right now") }
            return
        }
        val messages by messages.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }

        Column(modifier.imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(messages) { message -> MessageBubble(message) }
            }
            MessageInput(onSend = ::send)
        }
    }

    private fun send(text: String) {
        messages.update { it + ChatMessage(text, fromUser = true) }
        scope.launch {
            delay(REPLY_DELAY_MS)
            receive("Thanks, we got: \"$text\". An agent will reply soon.")
        }
    }

    private fun receive(text: String) {
        messages.update { it + ChatMessage(text, fromUser = false) }
        if (visibleConversations == 0) _unreadCount.update { it + 1 }
    }

    private const val REPLY_DELAY_MS = 2_000L
}

/** What the SDK needs from the host to connect – in a real SDK: channel/brand ids, keys, locale... */
internal data class ChatSdkConfig(
    val channelId: String,
    val welcomeMessage: String?,
)

private data class ChatMessage(val text: String, val fromUser: Boolean)

@Composable
private fun MessageBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(message.text, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun MessageInput(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message") },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                onSend(text.trim())
                text = ""
            },
            enabled = text.isNotBlank(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
