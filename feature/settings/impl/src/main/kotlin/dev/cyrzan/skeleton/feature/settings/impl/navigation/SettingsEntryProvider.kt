package dev.cyrzan.skeleton.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import dev.cyrzan.skeleton.feature.settings.impl.SettingsScreen
import javax.inject.Inject

internal class SettingsEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<SettingsNavKey> {
            SettingsScreen(onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface SettingsNavigationModule {

    @Binds
    @IntoSet
    fun bindSettingsEntryProvider(impl: SettingsEntryProvider): EntryProviderInstaller
}
