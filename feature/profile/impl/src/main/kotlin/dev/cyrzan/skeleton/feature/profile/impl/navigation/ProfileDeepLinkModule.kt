package dev.cyrzan.skeleton.feature.profile.impl.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.feature.profile.api.ProfileDeepLinks

@Module
@InstallIn(SingletonComponent::class)
internal object ProfileDeepLinkModule {

    @Provides
    @ElementsIntoSet
    fun provideProfileDeepLinks(): Set<DeepLinkRoute> = setOf(ProfileDeepLinks.profile, ProfileDeepLinks.about)
}
