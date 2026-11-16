package dev.cyrzan.skeleton.feature.invoices.impl.list

import dev.cyrzan.skeleton.core.file.CachedFile
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesError

/** Everything the screen draws. Survives configuration changes in the ViewModel; rebuilt after process death. */
internal data class InvoicesState(
    val isLoading: Boolean = true,
    /** Set when the list couldn't be loaded; the screen shows it with Retry. */
    val loadError: InvoicesError? = null,
    val invoices: List<Invoice> = emptyList(),
    /** One per row – several downloads can run at once. */
    val downloading: Set<InvoiceId> = emptySet(),
)

/** What the user did. The screen's only way to talk to the ViewModel. */
internal sealed interface InvoicesIntent {
    data object Retry : InvoicesIntent
    data class DownloadClicked(val id: InvoiceId) : InvoicesIntent
}

/** One-off effects the screen carries out and forgets. Anything that should still show after rotation belongs in [InvoicesState]. */
internal sealed interface InvoicesEvent {
    data class OpenFile(val file: CachedFile) : InvoicesEvent
    data class DownloadFailed(val error: InvoicesError) : InvoicesEvent
}
