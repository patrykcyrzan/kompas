package dev.cyrzan.skeleton.feature.home.impl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.feature.home.api.HomeDeepLinks

@Module
@InstallIn(SingletonComponent::class)
internal object HomeDeepLinkModule {

    @Provides
    @IntoSet
    fun provideHomeDeepLink(): DeepLinkRoute = HomeDeepLinks.home
}
