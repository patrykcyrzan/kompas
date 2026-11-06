package dev.cyrzan.skeleton.feature.pdfviewer.impl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfImporter
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfSource
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import javax.inject.Inject

internal class DefaultPdfImporter @Inject constructor(private val files: PdfFiles) : PdfImporter {

    override suspend fun fromBase64(base64: String, fileName: String?): PdfViewerNavKey {
        val file = files.importBase64(base64, fileName)
        return PdfViewerNavKey(PdfSource.Local(file.path), file.name)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PdfImporterModule {

    @Binds
    fun bindPdfImporter(impl: DefaultPdfImporter): PdfImporter
}
