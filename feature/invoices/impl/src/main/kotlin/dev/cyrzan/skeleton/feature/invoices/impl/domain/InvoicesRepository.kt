package dev.cyrzan.skeleton.feature.invoices.impl.domain

/**
 * What the domain needs from the data layer. The implementation (Retrofit, DTOs) lives in `data/`
 * and depends on this package – never the other way round.
 */
internal interface InvoicesRepository {
    suspend fun getInvoices(): Outcome<List<Invoice>, InvoicesError>
    suspend fun getDocument(id: InvoiceId): Outcome<InvoiceDocument, InvoicesError>
}
