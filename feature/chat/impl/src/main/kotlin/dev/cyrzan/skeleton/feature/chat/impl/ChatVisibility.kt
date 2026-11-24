package dev.cyrzan.skeleton.feature.chat.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration

/**
 * Whether the chat button is on screen – our product decision, not the SDK's. The SDK only has to be
 * able to connect (the app config switched the chat on); when to offer the chat is decided here.
 *
 * Today: [showDelay] after the chat gets switched on. Switched off = hidden at once, also during the delay;
 * switched on again = the delay starts over. A repeated "on" (every config refresh) does not restart it.
 */
internal class ChatVisibility(
    scope: CoroutineScope,
    private val showDelay: Duration,
) {

    private val enabled = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val isVisible: StateFlow<Boolean> = enabled
        // transformLatest: switching off cancels a pending delay.
        .transformLatest { on ->
            if (on) {
                delay(showDelay)
                emit(true)
            } else {
                emit(false)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Called with every config: `true` when the chat may be offered at all. */
    fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}
