package dev.cyrzan.skeleton.feature.invoices.impl.data

import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.InvoiceDocumentDto
import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.InvoiceDto
import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.InvoicesService
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesError
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.util.Base64

/** The data layer's job: DTO → domain, exceptions → [InvoicesError]. */
class DefaultInvoicesRepositoryTest {

    private val service = FakeInvoicesService()
    private val repository = DefaultInvoicesRepository(service)

    @Test
    fun `maps the DTO`() = runTest {
        service.invoices = { listOf(InvoiceDto(7, "FV/7", "2026-10-31", 12_300, "PLN")) }

        val invoice = (repository.getInvoices() as Outcome.Success).value.single()

        assertEquals(InvoiceId(7), invoice.id)
        assertEquals(LocalDate.of(2026, 10, 31), invoice.issuedOn)
    }

    @Test
    fun `decodes the document`() = runTest {
        val pdf = "%PDF-1.4".toByteArray()
        service.document = { InvoiceDocumentDto("a.pdf", "application/pdf", Base64.getEncoder().encodeToString(pdf)) }

        val document = (repository.getDocument(InvoiceId(7)) as Outcome.Success).value

        assertArrayEquals(pdf, document.content)
    }

    @Test
    fun `no connection is a network error`() = runTest {
        service.invoices = { throw IOException("offline") }

        assertEquals(Outcome.Failure(InvoicesError.Network), repository.getInvoices())
    }

    @Test
    fun `404 is not found, other HTTP errors are unknown`() = runTest {
        service.document = { throw httpError(404) }
        assertEquals(Outcome.Failure(InvoicesError.NotFound), repository.getDocument(InvoiceId(7)))

        service.document = { throw httpError(500) }
        assertEquals(Outcome.Failure(InvoicesError.Unknown), repository.getDocument(InvoiceId(7)))
    }

    @Test
    fun `a broken response is unknown`() = runTest {
        service.invoices = { listOf(InvoiceDto(7, "FV/7", "31.10.2026", 12_300, "PLN")) }

        assertEquals(Outcome.Failure(InvoicesError.Unknown), repository.getInvoices())
    }

    @Test
    fun `cancellation is not turned into an error`() = runTest {
        service.invoices = { throw CancellationException("left the screen") }

        val thrown = runCatching { repository.getInvoices() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}

private class FakeInvoicesService : InvoicesService {
    var invoices: suspend () -> List<InvoiceDto> = { emptyList() }
    var document: suspend () -> InvoiceDocumentDto = { error("not set") }

    override suspend fun getInvoices() = invoices()
    override suspend fun getDocument(id: Long) = document()
}
