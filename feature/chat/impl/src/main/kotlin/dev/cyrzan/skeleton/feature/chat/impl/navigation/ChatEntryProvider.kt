package dev.cyrzan.skeleton.feature.chat.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.fullScreen
import dev.cyrzan.skeleton.core.navigation.screenName
import dev.cyrzan.skeleton.feature.chat.api.ChatNavKey
import dev.cyrzan.skeleton.feature.chat.impl.ChatRoute
import javax.inject.Inject

internal class ChatEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        // Above the tabs: the conversation needs the whole height (keyboard + input).
        entry<ChatNavKey>(metadata = fullScreen() + screenName("chat")) {
            ChatRoute(onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface ChatNavigationModule {

    @Binds
    @IntoSet
    fun bindChatEntryProvider(impl: ChatEntryProvider): EntryProviderInstaller
}
