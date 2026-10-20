package dev.cyrzan.skeleton.app.deeplink

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.cyrzan.skeleton.app.BuildConfig
import dev.cyrzan.skeleton.core.deeplink.DebugOnlyGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.deeplink.InAppOnlyGuard
import dev.cyrzan.skeleton.core.deeplink.PendingDeepLinkStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DeepLinkModule {

    /** Routes come from `:feature:*:impl` (`@IntoSet`), guards from anywhere that needs one. */
    @Provides
    @Singleton
    fun provideDeepLinkResolver(
        routes: Set<@JvmSuppressWildcards DeepLinkRoute>,
        guards: Set<@JvmSuppressWildcards DeepLinkGuard>,
    ): DeepLinkResolver = createDeepLinkResolver(routes, guards)

    @Provides
    @Singleton
    fun providePendingDeepLinkStore(): PendingDeepLinkStore = PendingDeepLinkStore()

    @Provides
    @IntoSet
    fun provideDebugOnlyGuard(): DeepLinkGuard = DebugOnlyGuard(isDebugBuild = BuildConfig.DEBUG)

    @Provides
    @IntoSet
    fun provideInAppOnlyGuard(): DeepLinkGuard = InAppOnlyGuard()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DeepLinkMultibindings {

    @Multibinds
    fun deepLinkRoutes(): Set<DeepLinkRoute>

    @Multibinds
    fun deepLinkGuards(): Set<DeepLinkGuard>
}
