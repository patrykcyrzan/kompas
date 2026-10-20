package dev.cyrzan.skeleton.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.home.impl.HomeRoute
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import javax.inject.Inject

internal class HomeEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<HomeNavKey> {
            HomeRoute(
                onItemClick = { item -> navigator.navigate(DetailsNavKey(item.id)) },
                onSettingsClick = { navigator.navigate(SettingsNavKey) },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface HomeNavigationModule {

    @Binds
    @IntoSet
    fun bindHomeEntryProvider(impl: HomeEntryProvider): EntryProviderInstaller
}
