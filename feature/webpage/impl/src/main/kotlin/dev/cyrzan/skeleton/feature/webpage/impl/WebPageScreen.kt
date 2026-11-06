package dev.cyrzan.skeleton.feature.webpage.impl

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cyrzan.skeleton.core.navigation.OpenLinkResult
import dev.cyrzan.skeleton.core.webview.KompasWebView
import dev.cyrzan.skeleton.core.webview.WebContent
import dev.cyrzan.skeleton.core.webview.WebPageError
import dev.cyrzan.skeleton.core.webview.WebPageState
import dev.cyrzan.skeleton.core.webview.openExternally
import dev.cyrzan.skeleton.core.webview.rememberWebViewState
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import dev.cyrzan.skeleton.feature.webpage.api.WebPageNavKey

/**
 * System Back goes through the page's history first; the close button leaves the screen at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPageScreen(
    navKey: WebPageNavKey,
    openLink: (String) -> OpenLinkResult,
    onPdf: (PdfViewerNavKey) -> Unit,
    onCloseClick: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberWebViewState()
    val currentOpenLink by rememberUpdatedState(openLink)
    val urlHandlers = remember(context) {
        webPageUrlHandlers(openLink = { currentOpenLink(it) }, openInBrowser = { context.openExternally(it) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(navKey.title ?: state.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            KompasWebView(
                content = remember(navKey.url) { WebContent.Url(navKey.url) },
                state = state,
                modifier = Modifier.fillMaxSize(),
                urlHandlers = urlHandlers,
                onDownload = { download ->
                    val pdf = PdfViewerNavKey.fromDownload(download.url, download.contentDisposition, download.mimeType)
                    // Anything else (ZIP, DOCX…) – the browser downloads it.
                    if (pdf != null) onPdf(pdf) else context.openExternally(Uri.parse(download.url))
                },
            )
            when (val pageState = state.pageState) {
                is WebPageState.Loading -> LinearProgressIndicator(
                    progress = { pageState.progress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
                WebPageState.Loaded -> Unit
                // Covers the WebView's own error page – the user sees the app's message, in the app's style.
                is WebPageState.Error -> WebPageErrorContent(
                    error = pageState.error,
                    onRetryClick = state::reload,
                    onOpenInBrowserClick = { context.openExternally(Uri.parse(navKey.url)) },
                )
            }
        }
    }
}

@Composable
private fun WebPageErrorContent(error: WebPageError, onRetryClick: () -> Unit, onOpenInBrowserClick: () -> Unit) {
    // A Surface also takes the touches – nothing reaches the page below.
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                when (error) {
                    is WebPageError.Network -> "Couldn't load the page. Check your connection."
                    is WebPageError.Http -> "The page isn't available (${error.statusCode})."
                    WebPageError.Ssl -> "The connection to this page isn't secure."
                    WebPageError.RendererGone -> "The page stopped working."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetryClick) { Text("Try again") }
            // The browser may show what the app can't, e.g. its own error page. Never for SSL – no way around a bad certificate.
            if (error is WebPageError.Http) {
                OutlinedButton(onClick = onOpenInBrowserClick) { Text("Open in browser") }
            }
        }
    }
}
