package dev.cyrzan.skeleton.feature.chat.api

/**
 * How the app hands the chat its part of the global config response. The app only copies the fields –
 * whether that means "show the chat" is the chat's decision (e.g. enabled but without a channel = off).
 *
 * Call it after every config response; `null` (the response had no chat part) switches the chat off.
 * Until the first call the chat stays hidden. Main thread.
 */
interface ChatConfigurator {
    fun configure(config: ChatConfig?)
}

/** The chat part of the app config, as the backend sends it. */
data class ChatConfig(
    val enabled: Boolean,
    val channelId: String?,
    val welcomeMessage: String?,
)
