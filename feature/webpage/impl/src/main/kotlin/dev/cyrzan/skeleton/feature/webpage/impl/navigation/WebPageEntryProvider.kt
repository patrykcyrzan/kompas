package dev.cyrzan.skeleton.feature.webpage.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.webpage.api.WebPageNavKey
import dev.cyrzan.skeleton.feature.webpage.impl.WebPageScreen
import javax.inject.Inject

internal class WebPageEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<WebPageNavKey> { key ->
            WebPageScreen(
                navKey = key,
                openLink = navigator::openLink,
                onPdf = navigator::navigate,
                onCloseClick = { navigator.goBack() },
            )
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface WebPageNavigationModule {

    @Binds
    @IntoSet
    fun bindWebPageEntryProvider(impl: WebPageEntryProvider): EntryProviderInstaller
}
