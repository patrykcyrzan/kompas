package dev.cyrzan.skeleton.feature.invoices.impl.domain

import dev.cyrzan.skeleton.core.file.CachedFile
import dev.cyrzan.skeleton.core.file.FileStorage
import java.io.IOException
import javax.inject.Inject

/**
 * Fetches the invoice's PDF and puts it in the cache, ready to open or share.
 *
 * A use case because it joins two sources (backend + files). Plain reads – the list – go from the
 * ViewModel straight to the repository; a use case that only forwards a call adds nothing.
 */
internal class DownloadInvoiceUseCase @Inject constructor(
    private val repository: InvoicesRepository,
    private val fileStorage: FileStorage,
) {
    suspend operator fun invoke(id: InvoiceId): Outcome<CachedFile, InvoicesError> =
        when (val document = repository.getDocument(id)) {
            is Outcome.Failure -> document
            is Outcome.Success -> try {
                with(document.value) { Outcome.Success(fileStorage.saveToCache(fileName, mimeType, content)) }
            } catch (e: IOException) {
                // Full disk and the like. Cancellation is not an IOException – it passes through.
                Outcome.Failure(InvoicesError.Unknown)
            }
        }
}
