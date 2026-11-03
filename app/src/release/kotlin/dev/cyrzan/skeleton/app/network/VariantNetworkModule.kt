package dev.cyrzan.skeleton.app.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.core.network.ExtraInterceptors
import javax.inject.Singleton

/** Release counterpart of the debug module with the same name: no inspection, no mocks. */
@Module
@InstallIn(SingletonComponent::class)
internal object VariantNetworkModule {

    @Provides
    @Singleton
    fun provideExtraInterceptors(): ExtraInterceptors = ExtraInterceptors(emptyList())
}
