package dev.cyrzan.skeleton.feature.invoices.impl.data.remote

import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceDocument
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Base64

@Serializable
internal data class InvoiceDto(
    val id: Long,
    val number: String,
    /** ISO date, `2026-10-31`. */
    @SerialName("issued_at") val issuedAt: String,
    @SerialName("amount_minor") val amountMinor: Long,
    val currency: String,
)

@Serializable
internal data class InvoiceDocumentDto(
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("content_base64") val contentBase64: String,
)

internal fun InvoiceDto.toDomain() = Invoice(
    id = InvoiceId(id),
    number = number,
    issuedOn = LocalDate.parse(issuedAt),
    amountMinor = amountMinor,
    currency = currency,
)

/** The whole file in memory – fine for invoices. Big files: a `@Streaming` endpoint and `ResponseBody.byteStream()` into FileStorage. */
internal fun InvoiceDocumentDto.toDomain() = InvoiceDocument(
    fileName = fileName,
    mimeType = mimeType,
    // MIME decoder: tolerates line breaks some backends put into long Base64.
    content = Base64.getMimeDecoder().decode(contentBase64),
)
