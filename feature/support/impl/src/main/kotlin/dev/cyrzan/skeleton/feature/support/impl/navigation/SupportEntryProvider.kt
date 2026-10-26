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
import dev.cyrzan.skeleton.core.navigation.sharedViewModelScope
import dev.cyrzan.skeleton.feature.support.api.SupportRequestNavKey
import dev.cyrzan.skeleton.feature.support.impl.SupportDetailsRoute
import dev.cyrzan.skeleton.feature.support.impl.SupportSummaryRoute
import dev.cyrzan.skeleton.feature.support.impl.SupportTopicRoute
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Steps 2 and 3: internal – nobody outside the feature opens them, and they carry no arguments (the data is in the shared ViewModel). */
@Serializable
internal data object SupportDetailsNavKey : NavKey

@Serializable
internal data object SupportSummaryNavKey : NavKey

internal class SupportEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        // Every step shares ViewModels with the first one.
        val form = sharedViewModelScope(SupportRequestNavKey::class)

        entry<SupportRequestNavKey>(metadata = form) {
            SupportTopicRoute(
                onNext = { navigator.navigate(SupportDetailsNavKey) },
                onClose = { navigator.closeFlow() },
            )
        }
        entry<SupportDetailsNavKey>(metadata = form) {
            SupportDetailsRoute(
                onNext = { navigator.navigate(SupportSummaryNavKey) },
                onBack = { navigator.goBack() },
            )
        }
        entry<SupportSummaryNavKey>(metadata = form) {
            SupportSummaryRoute(
                // Leaving the flow clears the shared ViewModel – the next form starts empty.
                onDone = { navigator.closeFlow() },
                onBack = { navigator.goBack() },
            )
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
