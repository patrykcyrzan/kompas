package dev.cyrzan.skeleton.app.config

import dev.cyrzan.skeleton.feature.chat.api.ChatConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /v1/config` – one response for the whole app, with parts that belong to different modules. */
@Serializable
internal data class AppConfigDto(
    @SerialName("min_app_version") val minAppVersion: String? = null,
    val chat: ChatConfigDto? = null,
)

/** Defaults = off: a field the backend does not send must never switch the chat on. */
@Serializable
internal data class ChatConfigDto(
    val enabled: Boolean = false,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("welcome_message") val welcomeMessage: String? = null,
)

/** Fields only – what they mean is up to the chat. */
internal fun ChatConfigDto.toChatConfig() = ChatConfig(
    enabled = enabled,
    channelId = channelId,
    welcomeMessage = welcomeMessage,
)
