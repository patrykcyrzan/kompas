package dev.cyrzan.skeleton.app.config

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.feature.chat.api.ChatConfigurator
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * The app's global config request. Nobody observes the result: after each response the loader hands
 * every part to the module it belongs to – the chat part to [ChatConfigurator].
 */
internal class AppConfigLoader @Inject constructor(
    private val service: ConfigService,
    private val chat: ChatConfigurator,
) {

    /** Main thread (Retrofit's suspend calls do not block it). */
    suspend fun load() {
        val config = try {
            service.getConfig()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline, 5xx, unexpected JSON: nothing is passed on, so every module keeps its last state –
            // a flaky network must not switch the chat off.
            return
        }
        // A response without a chat part switches it off.
        chat.configure(config.chat?.toChatConfig())
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppConfigModule {

    @Provides
    @Singleton
    fun provideConfigService(retrofit: Retrofit): ConfigService = retrofit.create(ConfigService::class.java)
}
