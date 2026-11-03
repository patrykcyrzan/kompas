package dev.cyrzan.skeleton.feature.settings.impl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.feature.settings.api.SettingsDeepLinks

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDeepLinkModule {

    @Provides
    @IntoSet
    fun provideSettingsDeepLink(): DeepLinkRoute = SettingsDeepLinks.settings
}
