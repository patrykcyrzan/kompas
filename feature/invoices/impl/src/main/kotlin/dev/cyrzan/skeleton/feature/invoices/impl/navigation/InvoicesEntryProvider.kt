package dev.cyrzan.skeleton.feature.invoices.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.invoices.api.InvoicesNavKey
import dev.cyrzan.skeleton.feature.invoices.impl.list.InvoicesRoute
import javax.inject.Inject

internal class InvoicesEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<InvoicesNavKey> {
            InvoicesRoute(onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface InvoicesNavigationModule {

    @Binds
    @IntoSet
    fun bindInvoicesEntryProvider(impl: InvoicesEntryProvider): EntryProviderInstaller
}
