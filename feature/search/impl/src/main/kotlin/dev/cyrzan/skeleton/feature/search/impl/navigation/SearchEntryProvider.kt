package dev.cyrzan.skeleton.feature.search.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.deeplink.DeepLinkRoute
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchDeepLinks
import dev.cyrzan.skeleton.feature.search.api.SearchNavKey
import dev.cyrzan.skeleton.feature.search.api.SearchResultsNavKey
import dev.cyrzan.skeleton.feature.search.impl.SearchResultsRoute
import dev.cyrzan.skeleton.feature.search.impl.SearchScreen
import javax.inject.Inject

internal class SearchEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<SearchNavKey> { key ->
            SearchScreen(
                initial = key.initial,
                // The finished criteria are the argument of the next screen.
                onSearch = { criteria -> navigator.navigate(SearchResultsNavKey(criteria)) },
                onBackClick = { navigator.goBack() },
            )
        }
        entry<SearchResultsNavKey> { key ->
            SearchResultsRoute(
                navKey = key,
                onItemClick = { itemId -> navigator.navigate(DetailsNavKey(itemId)) },
                onBackClick = { navigator.goBack() },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface SearchNavigationModule {

    @Binds
    @IntoSet
    fun bindSearchEntryProvider(impl: SearchEntryProvider): EntryProviderInstaller
}

@Module
@InstallIn(SingletonComponent::class)
internal object SearchDeepLinkModule {

    @Provides
    @IntoSet
    fun provideSearchDeepLink(): DeepLinkRoute = SearchDeepLinks.results
}
