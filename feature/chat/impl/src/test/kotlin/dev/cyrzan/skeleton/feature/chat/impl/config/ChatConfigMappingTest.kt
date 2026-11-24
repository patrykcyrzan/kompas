package dev.cyrzan.skeleton.feature.chat.impl.config

import dev.cyrzan.skeleton.feature.chat.api.ChatConfig
import dev.cyrzan.skeleton.feature.chat.impl.sdk.ChatSdkConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatConfigMappingTest {

    @Test
    fun `no chat part in the response means no chat`() {
        assertNull((null as ChatConfig?).toSdkConfig())
    }

    @Test
    fun `switched off means no chat, even with a channel`() {
        assertNull(ChatConfig(enabled = false, channelId = "support", welcomeMessage = null).toSdkConfig())
    }

    @Test
    fun `switched on without a channel means no chat – the SDK could not connect`() {
        assertNull(ChatConfig(enabled = true, channelId = " ", welcomeMessage = null).toSdkConfig())
    }

    @Test
    fun `switched on with a channel configures the SDK`() {
        assertEquals(
            ChatSdkConfig(channelId = "support", welcomeMessage = "Hello"),
            ChatConfig(enabled = true, channelId = "support", welcomeMessage = "Hello").toSdkConfig(),
        )
    }
}
