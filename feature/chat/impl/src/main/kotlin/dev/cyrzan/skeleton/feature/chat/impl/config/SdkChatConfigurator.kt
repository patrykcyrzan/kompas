package dev.cyrzan.skeleton.feature.chat.impl.config

import dev.cyrzan.skeleton.feature.chat.api.ChatConfig
import dev.cyrzan.skeleton.feature.chat.api.ChatConfigurator
import dev.cyrzan.skeleton.feature.chat.impl.ChatVisibility
import dev.cyrzan.skeleton.feature.chat.impl.sdk.ChatSdkConfig
import dev.cyrzan.skeleton.feature.chat.impl.sdk.FakeChatSdk
import javax.inject.Inject

/**
 * Applies the app's config: the SDK gets what it needs to connect, [ChatVisibility] learns whether the chat
 * may be offered – when the button actually shows is up to it, not to the SDK. Both ignore a repeated
 * config, so the app may call this after every response.
 */
internal class SdkChatConfigurator @Inject constructor(
    private val visibility: ChatVisibility,
) : ChatConfigurator {

    override fun configure(config: ChatConfig?) {
        val sdkConfig = config.toSdkConfig()
        FakeChatSdk.configure(sdkConfig)
        visibility.setEnabled(sdkConfig != null)
    }
}

/**
 * What the SDK gets – `null` (chat off) unless the backend switched it on *and* sent what the SDK
 * needs to connect. No chat part in the response ([this] = `null`) is off as well.
 */
internal fun ChatConfig?.toSdkConfig(): ChatSdkConfig? {
    if (this == null || !enabled) return null
    val channel = channelId?.takeIf { it.isNotBlank() } ?: return null
    return ChatSdkConfig(channelId = channel, welcomeMessage = welcomeMessage?.takeIf { it.isNotBlank() })
}
