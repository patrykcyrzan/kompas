package dev.cyrzan.skeleton.feature.invoices.impl.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

internal interface InvoicesService {

    @GET("v1/invoices")
    suspend fun getInvoices(): List<InvoiceDto>

    @GET("v1/invoices/{id}/document")
    suspend fun getDocument(@Path("id") id: Long): InvoiceDocumentDto
}
