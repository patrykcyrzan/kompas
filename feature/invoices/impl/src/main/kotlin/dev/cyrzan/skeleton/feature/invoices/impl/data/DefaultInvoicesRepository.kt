package dev.cyrzan.skeleton.feature.invoices.impl.data

import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.InvoicesService
import dev.cyrzan.skeleton.feature.invoices.impl.data.remote.toDomain
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceDocument
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesError
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesRepository
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class DefaultInvoicesRepository @Inject constructor(
    private val service: InvoicesService,
) : InvoicesRepository {

    override suspend fun getInvoices(): Outcome<List<Invoice>, InvoicesError> = call {
        service.getInvoices().map { it.toDomain() }
    }

    override suspend fun getDocument(id: InvoiceId): Outcome<InvoiceDocument, InvoicesError> = call {
        val dto = service.getDocument(id.value)
        // Decoding a few MB of Base64 is CPU work – not on the main thread.
        withContext(Dispatchers.Default) { dto.toDomain() }
    }

    /** The one place where exceptions become [InvoicesError] – nothing above `data/` sees Retrofit or OkHttp. */
    private inline fun <T> call(block: () -> T): Outcome<T, InvoicesError> =
        try {
            Outcome.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Outcome.Failure(InvoicesError.Network)
        } catch (e: HttpException) {
            Outcome.Failure(if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) InvoicesError.NotFound else InvoicesError.Unknown)
        } catch (e: Exception) {
            // Unexpected JSON, a bad date or Base64 – a backend contract problem, not the user's.
            Outcome.Failure(InvoicesError.Unknown)
        }
}
