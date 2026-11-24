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
import dev.cyrzan.skeleton.core.navigation.screenName
import dev.cyrzan.skeleton.feature.auth.api.LoginNavKey
import dev.cyrzan.skeleton.feature.details.api.ItemPickerNavKey
import dev.cyrzan.skeleton.feature.invoices.api.InvoicesNavKey
import dev.cyrzan.skeleton.feature.legacy.api.LegacyNavKey
import dev.cyrzan.skeleton.feature.orders.api.OrdersNavKey
import dev.cyrzan.skeleton.feature.profile.api.AboutNavKey
import dev.cyrzan.skeleton.feature.profile.api.ProfileNavKey
import dev.cyrzan.skeleton.feature.profile.impl.AboutScreen
import dev.cyrzan.skeleton.feature.profile.impl.ProfileRoute
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey
import javax.inject.Inject

internal class ProfileEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<ProfileNavKey>(metadata = screenName("profile")) {
            ProfileRoute(
                // A full-screen key: opens above the tabs (decided by the app).
                onSignInClick = { navigator.navigate(LoginNavKey) },
                // Another feature's screen with an (optional) argument; its result comes back via ResultEffect.
                onFavouriteClick = { current -> navigator.navigate(ItemPickerNavKey(selected = current)) },
                // OrdersNavKey is a tab root: navigate() switches tabs instead of pushing it here.
                onOrdersClick = { navigator.navigate(OrdersNavKey) },
                onInvoicesClick = { navigator.navigate(InvoicesNavKey) },
                // A screen of :app, not of a feature – opened by its key all the same.
                onLegacyClick = { navigator.navigate(LegacyNavKey) },
                onSettingsClick = { navigator.navigate(SettingsNavKey) },
                onSupportClick = { navigator.navigate(SupportRequestNavKey()) },
                onAboutClick = { navigator.navigate(AboutNavKey) },
            )
        }
        entry<AboutNavKey>(metadata = screenName("about")) {
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
