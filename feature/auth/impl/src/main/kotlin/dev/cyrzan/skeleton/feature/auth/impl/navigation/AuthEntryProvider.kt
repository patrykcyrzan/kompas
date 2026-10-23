package dev.cyrzan.skeleton.feature.auth.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.auth.api.LoginNavKey
import dev.cyrzan.skeleton.feature.auth.impl.LoginRoute
import javax.inject.Inject

internal class AuthEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<LoginNavKey> {
            LoginRoute(
                onSignedIn = {
                    // Leave the sign-in flow first, then open the link that needed a session (if any).
                    navigator.closeFlow()
                    navigator.resumePendingLink()
                },
                onCloseClick = { navigator.goBack() },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface AuthNavigationModule {

    @Binds
    @IntoSet
    fun bindAuthEntryProvider(impl: AuthEntryProvider): EntryProviderInstaller
}
