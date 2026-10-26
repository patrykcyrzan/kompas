package dev.cyrzan.skeleton.feature.details.impl.navigation

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
import dev.cyrzan.skeleton.feature.details.impl.DetailsRoute
import javax.inject.Inject

internal class DetailsEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<DetailsNavKey> { key ->
            DetailsRoute(
                navKey = key,
                // The same screen again, with other arguments – pushed on the current stack.
                onRelatedClick = { itemId -> navigator.navigate(DetailsNavKey(itemId)) },
                onBackClick = { navigator.goBack() },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface DetailsNavigationModule {

    @Binds
    @IntoSet
    fun bindDetailsEntryProvider(impl: DetailsEntryProvider): EntryProviderInstaller
}
