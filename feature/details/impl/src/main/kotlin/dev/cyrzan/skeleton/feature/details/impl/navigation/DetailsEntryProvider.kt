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
import dev.cyrzan.skeleton.feature.details.api.ItemPickerNavKey
import dev.cyrzan.skeleton.feature.details.api.ItemPickerResult
import dev.cyrzan.skeleton.feature.details.impl.DetailsRoute
import dev.cyrzan.skeleton.feature.search.api.SearchCriteria
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.feature.details.impl.ItemPickerRoute
import javax.inject.Inject

internal class DetailsEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<DetailsNavKey> { key ->
            DetailsRoute(
                navKey = key,
                // The same screen again, with other arguments – pushed on the current stack.
                onRelatedClick = { itemId -> navigator.navigate(DetailsNavKey(itemId)) },
                // Another feature's screen with a value-object argument.
                onCategoryClick = { category -> navigator.navigate(SearchResultsNavKey(SearchCriteria(category = category))) },
                onBackClick = { navigator.goBack() },
            )
        }
        entry<ItemPickerNavKey> { key ->
            ItemPickerRoute(
                selected = key.selected,
                // Back to the screen that opened the picker, with the choice.
                onPick = { itemId -> navigator.goBackWithResult(ItemPickerResult, itemId) },
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
