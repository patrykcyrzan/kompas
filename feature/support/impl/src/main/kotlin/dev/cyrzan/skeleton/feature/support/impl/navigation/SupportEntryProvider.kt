package dev.cyrzan.skeleton.feature.support.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.fullScreen
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey
import dev.cyrzan.skeleton.feature.support.impl.SupportRoute
import javax.inject.Inject

internal class SupportEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        // One entry for the whole 3-step form, opened above the tabs.
        entry<SupportRequestNavKey>(metadata = fullScreen()) { key ->
            // Leaving the flow clears the form's ViewModel – the next form starts empty.
            SupportRoute(navKey = key, onClose = { navigator.closeFlow() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface SupportNavigationModule {

    @Binds
    @IntoSet
    fun bindSupportEntryProvider(impl: SupportEntryProvider): EntryProviderInstaller
}
