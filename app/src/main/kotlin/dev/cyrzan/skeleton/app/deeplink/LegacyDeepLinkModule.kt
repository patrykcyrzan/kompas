package dev.cyrzan.skeleton.app.deeplink

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.app.legacy.SessionManager
import dev.cyrzan.skeleton.core.deeplink.AuthenticationGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkGuard
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute

/** Legacy routes join the same Set<DeepLinkRoute> as routes from feature modules. */
@Module
@InstallIn(SingletonComponent::class)
internal object LegacyDeepLinkModule {

    @Provides
    @ElementsIntoSet
    fun provideLegacyRoutes(): Set<DeepLinkRoute> = LegacyDeepLinks.all

    @Provides
    @IntoSet
    fun provideAuthenticationGuard(session: SessionManager): DeepLinkGuard =
        AuthenticationGuard(isAuthenticated = session::isLoggedIn, loginDestination = LegacyDestination.Login)
}
