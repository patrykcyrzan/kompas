package dev.cyrzan.skeleton.feature.pdfviewer.impl

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2f

@Composable
internal fun PdfViewerRoute(
    navKey: PdfViewerNavKey,
    onBackClick: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel<PdfViewerViewModel, PdfViewerViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // The system's "Save as": the user picks the folder (Downloads, Drive, …) and may rename. No storage permission.
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PdfViewerNavKey.PDF_MIME_TYPE)) { uri ->
        uri?.let(viewModel::saveTo)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(
                when (message) {
                    PdfViewerMessage.Saved -> "Saved"
                    PdfViewerMessage.SaveFailed -> "Couldn't save the file"
                },
            )
        }
    }

    PdfViewerScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onShareClick = {
            viewModel.shareIntent()?.let { intent ->
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Nothing to share to – practically only on stripped-down devices.
                }
            }
        },
        onSaveClick = { saveLauncher.launch(uiState.title) },
        onRetryClick = viewModel::retry,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            PdfActionsBar(enabled = uiState is PdfViewerUiState.Ready, onShareClick = onShareClick, onSaveClick = onSaveClick)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (uiState) {
                is PdfViewerUiState.Loading -> CircularProgressIndicator()
                is PdfViewerUiState.Ready -> PdfPages(uiState.document, Modifier.fillMaxSize())
                is PdfViewerUiState.Error -> PdfErrorContent(uiState.error, uiState.canRetry, onRetryClick)
            }
        }
    }
}

/** Share and Save side by side; disabled until there is a PDF to share or save. */
@Composable
private fun PdfActionsBar(enabled: Boolean, onShareClick: () -> Unit, onSaveClick: () -> Unit) {
    BottomAppBar(contentPadding = PaddingValues(horizontal = 16.dp)) {
        OutlinedButton(onClick = onShareClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Share")
        }
        Spacer(Modifier.width(12.dp))
        Button(onClick = onSaveClick, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text("Save")
        }
    }
}

@Composable
private fun PdfErrorContent(error: PdfError, canRetry: Boolean, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            when (error) {
                PdfError.Download -> "Couldn't download the document"
                PdfError.PasswordProtected -> "This document is password-protected"
                PdfError.NotAPdf -> "Couldn't open the document"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (canRetry) {
            Button(onClick = onRetryClick) { Text("Try again") }
        }
    }
}

/**
 * Pages one under another, rendered only while visible. Pinch or double-tap to zoom; when zoomed, one finger
 * pans sideways and the list still scrolls vertically. Pages are rendered at screen width, so they get soft at high zoom.
 */
@Composable
private fun PdfPages(document: PdfDocument, modifier: Modifier = Modifier) {
    var scale by remember(document) { mutableFloatStateOf(1f) }
    var offsetX by remember(document) { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(document) {
                fun maxOffset(scale: Float) = (scale - 1f) * size.width / 2f
                // Initial pass: two fingers must zoom before the list takes them as a scroll.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pan = event.calculatePan()
                        if (event.changes.count { it.pressed } > 1) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset(scale), maxOffset(scale))
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            // Not consumed: the list keeps the vertical part.
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset(scale), maxOffset(scale))
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(document) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        offsetX = 0f
                    },
                )
            },
    ) {
        val pageWidth = constraints.maxWidth
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                },
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(document.pageCount) { index ->
                PdfPage(document, index, pageWidth)
            }
        }
    }
}

@Composable
private fun PdfPage(document: PdfDocument, index: Int, width: Int) {
    val bitmap by produceState<ImageBitmap?>(null, document, index, width) {
        value = document.renderPage(index, width).asImageBitmap()
    }
    // Space reserved up front, so the list doesn't jump while pages render.
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(document.pageAspectRatios[index])
            .background(Color.White),
    ) {
        bitmap?.let { Image(it, contentDescription = "Page ${index + 1} of ${document.pageCount}", modifier = Modifier.fillMaxSize()) }
    }
}
