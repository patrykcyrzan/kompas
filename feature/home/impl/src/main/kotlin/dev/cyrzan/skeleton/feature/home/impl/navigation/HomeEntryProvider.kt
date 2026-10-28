package dev.cyrzan.skeleton.feature.home.impl.navigation

import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.feature.details.api.DetailsNavKey
import dev.cyrzan.skeleton.feature.home.api.HomeNavKey
import dev.cyrzan.skeleton.feature.home.impl.HomeRoute
import dev.cyrzan.skeleton.feature.search.api.SearchNavKey
import dev.cyrzan.skeleton.feature.settings.api.SettingsNavKey
import javax.inject.Inject

internal class HomeEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<HomeNavKey> {
            val uriHandler = LocalUriHandler.current
            HomeRoute(
                onItemClick = { item -> navigator.navigate(DetailsNavKey(item.id)) },
                onPromoClick = { promo ->
                    // A link from the backend goes through the same validation and guards as external ones.
                    val result = navigator.openLink(promo.link)
                    // Not an app link -> open it outside, but only plain https (never intent://, file://, javascript: ...).
                    // Blocked links are NOT opened anywhere else.
                    if (result == OpenLinkResult.Unsupported && promo.link.startsWith("https://")) {
                        uriHandler.openUri(promo.link)
                    }
                },
                onSearchClick = { navigator.navigate(SearchNavKey()) },
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
