package dev.cyrzan.skeleton.feature.details.impl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.feature.details.api.DetailsDeepLinks

@Module
@InstallIn(SingletonComponent::class)
internal object DetailsDeepLinkModule {

    @Provides
    @IntoSet
    fun provideItemDeepLink(): DeepLinkRoute = DetailsDeepLinks.item
}
