package dev.cyrzan.skeleton.feature.chat.impl

import android.app.Application
import dev.cyrzan.skeleton.core.startup.AppInitializer
import dev.cyrzan.skeleton.feature.chat.impl.sdk.FakeChatSdk
import javax.inject.Inject

/**
 * `FakeChatSdk.initialize(this)` from the Application – the only place it is called. Initialized is not
 * configured: the chat stays hidden until the app passes the config on (ChatConfigurator).
 */
internal class ChatInitializer @Inject constructor() : AppInitializer {

    override fun init(application: Application) {
        FakeChatSdk.initialize(application)
    }
}
