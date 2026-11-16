package dev.cyrzan.skeleton.feature.invoices.impl.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.feature.invoices.impl.domain.DownloadInvoiceUseCase
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesRepository
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Outcome
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class InvoicesViewModel @Inject constructor(
    private val repository: InvoicesRepository,
    private val downloadInvoice: DownloadInvoiceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InvoicesState())
    val state: StateFlow<InvoicesState> = _state.asStateFlow()

    // A Channel, not a SharedFlow: an event sent while the screen is in the background waits for it instead of being lost.
    private val _events = Channel<InvoicesEvent>(Channel.BUFFERED)
    val events: Flow<InvoicesEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: InvoicesIntent) {
        when (intent) {
            InvoicesIntent.Retry -> load()
            is InvoicesIntent.DownloadClicked -> download(intent.id)
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getInvoices()) {
                is Outcome.Success -> _state.update { it.copy(isLoading = false, invoices = result.value) }
                is Outcome.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error) }
            }
        }
    }

    private fun download(id: InvoiceId) {
        // A second tap on the same row while it downloads does nothing.
        if (id in _state.value.downloading) return
        _state.update { it.copy(downloading = it.downloading + id) }
        viewModelScope.launch {
            val event = when (val result = downloadInvoice(id)) {
                is Outcome.Success -> InvoicesEvent.OpenFile(result.value)
                is Outcome.Failure -> InvoicesEvent.DownloadFailed(result.error)
            }
            _state.update { it.copy(downloading = it.downloading - id) }
            _events.send(event)
        }
    }
}
