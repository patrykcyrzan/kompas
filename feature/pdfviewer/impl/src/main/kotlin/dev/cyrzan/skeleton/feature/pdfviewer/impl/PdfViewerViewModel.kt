package dev.cyrzan.skeleton.feature.pdfviewer.impl

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfFileName
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfSource
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

/** Gets the PDF once per screen (download or imported file); the file and the open document go away with the ViewModel. */
@HiltViewModel(assistedFactory = PdfViewerViewModel.Factory::class)
internal class PdfViewerViewModel @AssistedInject constructor(
    @Assisted private val navKey: PdfViewerNavKey,
    private val files: PdfFiles,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Loading(title = navKey.fileName ?: initialTitle(navKey.source)))
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private val _messages = Channel<PdfViewerMessage>(Channel.BUFFERED)
    /** One-off messages for a snackbar. */
    val messages: Flow<PdfViewerMessage> = _messages.receiveAsFlow()

    private var file: File? = null
    private var document: PdfDocument? = null
    private var loading: Job? = null

    /** Only a download can go better the next time; a local file stays what it is. */
    private val canRetry = navKey.source is PdfSource.Remote

    init {
        load()
    }

    fun retry() {
        if (loading?.isActive == true || !canRetry) return
        _uiState.value = PdfViewerUiState.Loading(_uiState.value.title)
        load()
    }

    /** `null` until the PDF is downloaded. */
    fun shareIntent(): Intent? = file?.let(files::shareIntent)

    /** [target] – the document the user created in the system's "Save as" picker. */
    fun saveTo(target: Uri) {
        val file = file ?: return
        viewModelScope.launch {
            val saved = try {
                files.saveTo(file, target)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _messages.send(if (saved) PdfViewerMessage.Saved else PdfViewerMessage.SaveFailed)
        }
    }

    private fun load() {
        // A retry after "not a PDF" downloads again.
        file?.let(files::delete)
        file = null
        loading = viewModelScope.launch {
            val downloaded = try {
                files.fetch(navKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A local file can only be missing – e.g. the system cleared the cache while the app was in the background.
                val error = if (canRetry) PdfError.Download else PdfError.NotAPdf
                _uiState.value = PdfViewerUiState.Error(_uiState.value.title, error, canRetry)
                return@launch
            }
            file = downloaded
            _uiState.value = try {
                val opened = PdfDocument.open(downloaded)
                document = opened
                PdfViewerUiState.Ready(downloaded.name, opened)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                // A password won't change on retry.
                PdfViewerUiState.Error(downloaded.name, PdfError.PasswordProtected, canRetry = false)
            } catch (e: Exception) {
                // Often an HTML page (a login form, an error) served instead of the PDF.
                PdfViewerUiState.Error(downloaded.name, PdfError.NotAPdf, canRetry)
            }
        }
    }

    override fun onCleared() {
        document?.close()
        // An imported file is the viewer's even if it never opened.
        (file ?: (navKey.source as? PdfSource.Local)?.let { File(it.path) })?.let(files::delete)
    }

    private fun initialTitle(source: PdfSource): String = when (source) {
        is PdfSource.Remote -> PdfFileName.from(source.url, null)
        is PdfSource.Local -> File(source.path).name
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: PdfViewerNavKey): PdfViewerViewModel
    }
}

internal sealed interface PdfViewerUiState {
    /** The file name – also the suggested name in "Save as". */
    val title: String

    data class Loading(override val title: String) : PdfViewerUiState
    data class Ready(override val title: String, val document: PdfDocument) : PdfViewerUiState
    data class Error(override val title: String, val error: PdfError, val canRetry: Boolean) : PdfViewerUiState
}

internal enum class PdfError { Download, PasswordProtected, NotAPdf }

internal enum class PdfViewerMessage { Saved, SaveFailed }
