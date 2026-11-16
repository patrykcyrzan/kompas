package dev.cyrzan.skeleton.feature.invoices.impl.list

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.cyrzan.skeleton.core.file.viewIntent
import dev.cyrzan.skeleton.feature.invoices.impl.domain.Invoice
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoiceId
import dev.cyrzan.skeleton.feature.invoices.impl.domain.InvoicesError
import dev.cyrzan.skeleton.feature.invoices.impl.list.components.InvoiceRow
import java.time.LocalDate

/** Wiring: ViewModel, one-off events, Android APIs. Everything visual is in [InvoicesScreen]. */
@Composable
internal fun InvoicesRoute(
    onBackClick: () -> Unit,
    viewModel: InvoicesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel, lifecycle) {
        // Only while visible: no startActivity from the background; events sent meanwhile wait in the Channel.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is InvoicesEvent.OpenFile -> try {
                        context.startActivity(event.file.viewIntent())
                    } catch (e: ActivityNotFoundException) {
                        snackbarHostState.showSnackbar("No app can open ${event.file.name}")
                    }
                    is InvoicesEvent.DownloadFailed -> snackbarHostState.showSnackbar("Couldn't download the invoice. ${event.error.message}")
                }
            }
        }
    }

    InvoicesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
    )
}

/** Stateless: draws [state], reports what the user did through [onIntent]. Previewable without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InvoicesScreen(
    state: InvoicesState,
    snackbarHostState: SnackbarHostState,
    onIntent: (InvoicesIntent) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when {
            state.isLoading -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.loadError != null -> Column(
                modifier,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Couldn't load your invoices. ${state.loadError.message}")
                Button(onClick = { onIntent(InvoicesIntent.Retry) }) { Text("Try again") }
            }
            state.invoices.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) { Text("No invoices yet") }
            else -> LazyColumn(modifier) {
                items(state.invoices, key = { it.id.value }) { invoice ->
                    InvoiceRow(
                        invoice = invoice,
                        isDownloading = invoice.id in state.downloading,
                        onDownloadClick = { onIntent(InvoicesIntent.DownloadClicked(invoice.id)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** The error in the user's words – mapping errors to text is the UI's job, not the domain's. */
private val InvoicesError.message: String
    get() = when (this) {
        InvoicesError.Network -> "Check your connection."
        InvoicesError.NotFound -> "It's no longer available."
        InvoicesError.Unknown -> "Try again later."
    }

@Preview
@Composable
private fun InvoicesScreenPreview() {
    val invoices = listOf(
        Invoice(InvoiceId(1), "FV/2026/10/001", LocalDate.of(2026, 10, 31), 12_300, "PLN"),
        Invoice(InvoiceId(2), "FV/2026/09/001", LocalDate.of(2026, 9, 30), 9_900, "PLN"),
    )
    InvoicesScreen(
        state = InvoicesState(isLoading = false, invoices = invoices, downloading = setOf(InvoiceId(2))),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        onBackClick = {},
    )
}
