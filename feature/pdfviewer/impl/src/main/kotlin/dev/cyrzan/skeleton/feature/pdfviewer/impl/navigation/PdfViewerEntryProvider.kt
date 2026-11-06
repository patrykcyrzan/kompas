package dev.cyrzan.skeleton.feature.pdfviewer.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import dev.cyrzan.skeleton.feature.pdfviewer.impl.PdfViewerRoute
import javax.inject.Inject

internal class PdfViewerEntryProvider @Inject constructor() : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<PdfViewerNavKey> { key ->
            PdfViewerRoute(navKey = key, onBackClick = { navigator.goBack() })
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface PdfViewerNavigationModule {

    @Binds
    @IntoSet
    fun bindPdfViewerEntryProvider(impl: PdfViewerEntryProvider): EntryProviderInstaller
}
