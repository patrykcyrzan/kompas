package dev.cyrzan.skeleton.app.legacy

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
import dev.cyrzan.skeleton.feature.legacy.api.LegacyNavKey
import javax.inject.Inject

/** `:app`'s own screens – the "monolith" part – join the graph the same way as the features'. */
internal class LegacyEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<LegacyNavKey>(metadata = screenName("legacy")) {
            LegacyScreen(onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface LegacyNavigationModule {

    @Binds
    @IntoSet
    fun bindLegacyEntryProvider(impl: LegacyEntryProvider): EntryProviderInstaller
}
