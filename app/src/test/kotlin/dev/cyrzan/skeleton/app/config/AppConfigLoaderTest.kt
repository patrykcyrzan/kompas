package dev.cyrzan.skeleton.app.config

import dev.cyrzan.skeleton.feature.chat.api.ChatConfig
import dev.cyrzan.skeleton.feature.chat.api.ChatConfigurator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AppConfigLoaderTest {

    private class FakeConfigService(var answer: () -> AppConfigDto) : ConfigService {
        override suspend fun getConfig(): AppConfigDto = answer()
    }

    private class RecordingChatConfigurator : ChatConfigurator {
        val calls = mutableListOf<ChatConfig?>()
        override fun configure(config: ChatConfig?) {
            calls += config
        }
    }

    private val chat = RecordingChatConfigurator()

    @Test
    fun `the chat part of the response is passed to the chat`() = runTest {
        val service = FakeConfigService {
            AppConfigDto(chat = ChatConfigDto(enabled = true, channelId = "support", welcomeMessage = "Hello"))
        }

        AppConfigLoader(service, chat).load()

        assertEquals(listOf(ChatConfig(enabled = true, channelId = "support", welcomeMessage = "Hello")), chat.calls)
    }

    @Test
    fun `a response without a chat part switches the chat off`() = runTest {
        AppConfigLoader(FakeConfigService { AppConfigDto() }, chat).load()

        assertEquals(listOf<ChatConfig?>(null), chat.calls)
    }

    @Test
    fun `a failed request passes nothing on, so the chat keeps its last state`() = runTest {
        AppConfigLoader(FakeConfigService { throw IOException("offline") }, chat).load()

        assertEquals(emptyList<ChatConfig?>(), chat.calls)
    }
}
