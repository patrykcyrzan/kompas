package dev.cyrzan.skeleton.feature.profile.impl.navigation

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
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.profile.api.AboutNavKey
import dev.cyrzan.skeleton.feature.profile.api.ProfileNavKey
import dev.cyrzan.skeleton.feature.profile.impl.AboutScreen
import dev.cyrzan.skeleton.feature.profile.impl.ProfileRoute
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import javax.inject.Inject

internal class ProfileEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<ProfileNavKey> {
            ProfileRoute(
                // A full-screen key: opens above the tabs (decided by the app).
                onSignInClick = { navigator.navigate(LoginNavKey) },
                // OrdersNavKey is a tab root: navigate() switches tabs instead of pushing it here.
                onOrdersClick = { navigator.navigate(OrdersNavKey) },
                onSettingsClick = { navigator.navigate(SettingsNavKey) },
                onAboutClick = { navigator.navigate(AboutNavKey) },
            )
        }
        entry<AboutNavKey> {
            AboutScreen(onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface ProfileNavigationModule {

    @Binds
    @IntoSet
    fun bindProfileEntryProvider(impl: ProfileEntryProvider): EntryProviderInstaller
}
