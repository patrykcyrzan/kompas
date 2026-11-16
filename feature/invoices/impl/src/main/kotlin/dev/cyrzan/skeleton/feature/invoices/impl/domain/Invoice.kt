package dev.cyrzan.skeleton.feature.invoices.impl.domain

import java.time.LocalDate

@JvmInline
internal value class InvoiceId(val value: Long)

/** What the screens work with – no JSON names, no backend formats. */
internal data class Invoice(
    val id: InvoiceId,
    val number: String,
    val issuedOn: LocalDate,
    /** In the currency's smallest unit (grosze, cents). */
    val amountMinor: Long,
    val currency: String,
)

/** Not a data class: `equals` on a [ByteArray] compares references. */
internal class InvoiceDocument(
    val fileName: String,
    val mimeType: String,
    val content: ByteArray,
)
