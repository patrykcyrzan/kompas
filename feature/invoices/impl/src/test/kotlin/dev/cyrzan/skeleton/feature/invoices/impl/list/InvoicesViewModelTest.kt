package dev.cyrzan.skeleton.feature.invoices.impl.list

import dev.cyrzan.skeleton.core.file.CachedFile
import dev.cyrzan.skeleton.core.file.FileStorage
import dev.cyrzan.skeleton.feature.invoices.impl.domain.DownloadInvoiceUseCase
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceDocument
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesError
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesRepository
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Outcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.time.LocalDate

/**
 * MVI in a test: send intents, check state and events. No Android, no Hilt.
 * A successful download is not covered here: [CachedFile] holds an `android.net.Uri`, which plain JVM tests cannot create.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InvoicesViewModelTest {

    private val invoice = Invoice(InvoiceId(1), "FV/1", LocalDate.of(2026, 10, 31), 12_300, "PLN")
    private val repository = FakeInvoicesRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = InvoicesViewModel(repository, DownloadInvoiceUseCase(repository, UnusedFileStorage))

    @Test
    fun `loads invoices on start`() {
        repository.invoices = { Outcome.Success(listOf(invoice)) }

        val state = viewModel().state.value

        assertEquals(false, state.isLoading)
        assertEquals(listOf(invoice), state.invoices)
    }

    @Test
    fun `retry after a failed load`() {
        repository.invoices = { Outcome.Failure(InvoicesError.Network) }
        val viewModel = viewModel()
        assertEquals(InvoicesError.Network, viewModel.state.value.loadError)

        repository.invoices = { Outcome.Success(listOf(invoice)) }
        viewModel.onIntent(InvoicesIntent.Retry)

        assertNull(viewModel.state.value.loadError)
        assertEquals(listOf(invoice), viewModel.state.value.invoices)
    }

    @Test
    fun `row shows progress while downloading, then the error is reported once`() = runTest {
        repository.invoices = { Outcome.Success(listOf(invoice)) }
        val response = CompletableDeferred<Outcome<InvoiceDocument, InvoicesError>>()
        repository.document = { response.await() }
        val viewModel = viewModel()

        viewModel.onIntent(InvoicesIntent.DownloadClicked(invoice.id))
        assertEquals(setOf(invoice.id), viewModel.state.value.downloading)

        response.complete(Outcome.Failure(InvoicesError.NotFound))
        assertEquals(InvoicesEvent.DownloadFailed(InvoicesError.NotFound), viewModel.events.first())
        assertEquals(emptySet<InvoiceId>(), viewModel.state.value.downloading)
    }

    @Test
    fun `second tap on a downloading row is ignored`() {
        repository.invoices = { Outcome.Success(listOf(invoice)) }
        repository.document = { CompletableDeferred<Outcome<InvoiceDocument, InvoicesError>>().await() }
        val viewModel = viewModel()

        viewModel.onIntent(InvoicesIntent.DownloadClicked(invoice.id))
        viewModel.onIntent(InvoicesIntent.DownloadClicked(invoice.id))

        assertEquals(1, repository.documentCalls)
    }
}

private class FakeInvoicesRepository : InvoicesRepository {
    var invoices: suspend () -> Outcome<List<Invoice>, InvoicesError> = { Outcome.Success(emptyList()) }
    var document: suspend () -> Outcome<InvoiceDocument, InvoicesError> = { error("not set") }
    var documentCalls = 0

    override suspend fun getInvoices() = invoices()

    override suspend fun getDocument(id: InvoiceId): Outcome<InvoiceDocument, InvoicesError> {
        documentCalls++
        return document()
    }
}

private object UnusedFileStorage : FileStorage {
    override suspend fun saveToCache(fileName: String, mimeType: String, content: InputStream): CachedFile = error("not used")
    override suspend fun clearCache() = Unit
}
