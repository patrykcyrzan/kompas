package dev.cyrzan.skeleton.feature.chat.impl.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.startup.AppInitializer
import dev.cyrzan.skeleton.feature.chat.api.ChatButton
import dev.cyrzan.skeleton.feature.chat.api.ChatConfigurator
import dev.cyrzan.skeleton.feature.chat.impl.ChatInitializer
import dev.cyrzan.skeleton.feature.chat.impl.ChatVisibility
import dev.cyrzan.skeleton.feature.chat.impl.SdkChatButton
import dev.cyrzan.skeleton.feature.chat.impl.config.SdkChatConfigurator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
internal interface ChatModule {

    @Binds
    @IntoSet
    fun bindChatInitializer(impl: ChatInitializer): AppInitializer

    @Binds
    fun bindChatButton(impl: SdkChatButton): ChatButton

    @Binds
    fun bindChatConfigurator(impl: SdkChatConfigurator): ChatConfigurator

    companion object {
        /** Our decision: the chat is offered 10 s after the config switches it on. */
        @Provides
        @Singleton
        fun provideChatVisibility(): ChatVisibility = ChatVisibility(
            // Lives as long as the process, like the singleton itself. Main: read by the UI.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            showDelay = 10.seconds,
        )
    }
}
