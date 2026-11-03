package dev.cyrzan.skeleton.sandbox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import dev.cyrzan.skeleton.core.deeplink.BackStackPolicy
import dev.cyrzan.skeleton.core.deeplink.DeepLinkOrigin
import dev.cyrzan.skeleton.core.deeplink.DeepLinkResolver
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.network.ExtraInterceptors
import javax.inject.Singleton

/** What `:app` normally provides to the features, in its simplest form. */
@Module
@InstallIn(SingletonComponent::class)
internal object SandboxModule {

    /** Only the sandboxed feature's routes (its impl contributes them `@IntoSet`). */
    @Provides
    @Singleton
    fun provideDeepLinkResolver(routes: Set<@JvmSuppressWildcards DeepLinkRoute>): DeepLinkResolver =
        DeepLinkResolver(
            // Same origins as DEEP_LINK_ORIGINS in :app.
            origins = listOf(DeepLinkOrigin.Web(host = "kompas.example.com"), DeepLinkOrigin.CustomScheme(scheme = "kompas")),
            routes = routes,
            // In-app links only push their target, the synthetic back stack is never used.
            backStackPolicy = BackStackPolicy { destination -> listOf(destination) },
        )

    /** No Chucker and no mocks: the sandbox runs on the fake repositories. */
    @Provides
    @Singleton
    fun provideExtraInterceptors(): ExtraInterceptors = ExtraInterceptors(emptyList())
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SandboxMultibindings {

    /** A feature without deep links contributes none. */
    @Multibinds
    fun deepLinkRoutes(): Set<DeepLinkRoute>
}
