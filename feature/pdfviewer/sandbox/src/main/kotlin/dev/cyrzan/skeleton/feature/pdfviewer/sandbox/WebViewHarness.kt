package dev.cyrzan.skeleton.feature.pdfviewer.sandbox

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import dev.cyrzan.skeleton.core.navigation.EntryProviderInstaller
import dev.cyrzan.skeleton.core.navigation.Navigator
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfImporter
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sandbox-only screen: a web page with PDFs, wired the way any WebView in the app should be.
 * - A PDF link: the WebView can't show a PDF, so it reports it as a download and the viewer downloads it.
 * - A PDF the page made itself (no URL): the page passes it as Base64 to a `@JavascriptInterface`
 *   and [PdfImporter] hands it to the viewer.
 */
@kotlinx.serialization.Serializable
internal data object WebViewHarnessNavKey : NavKey

internal class WebViewHarnessEntryProvider @Inject constructor(
    private val pdfImporter: PdfImporter,
) : EntryProviderInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<WebViewHarnessNavKey> {
            WebViewHarnessScreen(pdfImporter, onPdf = navigator::navigate)
        }
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
internal interface WebViewHarnessModule {

    @Binds
    @IntoSet
    fun bindWebViewHarnessEntryProvider(impl: WebViewHarnessEntryProvider): EntryProviderInstaller
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewHarnessScreen(pdfImporter: PdfImporter, onPdf: (PdfViewerNavKey) -> Unit) {
    val scope = rememberCoroutineScope()
    AndroidView(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                    PdfViewerNavKey.fromDownload(url, contentDisposition, mimeType)?.let(onPdf)
                }
                addJavascriptInterface(PdfBridge(this, scope, pdfImporter, onPdf), "KompasPdf")
                loadDataWithBaseURL(TRUSTED_ORIGIN, HTML, "text/html", "utf-8", TRUSTED_ORIGIN)
            }
        },
    )
}

/** `window.KompasPdf.onPdf(base64, filename)` – the shape a web team typically gives the app. */
private class PdfBridge(
    private val webView: WebView,
    private val scope: CoroutineScope,
    private val pdfImporter: PdfImporter,
    private val onPdf: (PdfViewerNavKey) -> Unit,
) {
    /** Called on the WebView's JavaBridge thread, not Main. */
    @JavascriptInterface
    fun onPdf(base64: String?, filename: String?) {
        base64 ?: return
        scope.launch {
            // Every page and frame in this WebView sees the bridge – only our own may hand the user a document.
            // webView.url must be read on Main.
            if (!webView.url.orEmpty().startsWith(TRUSTED_ORIGIN)) return@launch
            val key = try {
                pdfImporter.fromBase64(base64, filename)
            } catch (e: IllegalArgumentException) {
                return@launch // Not Base64 – in the app: log it, the web side has a bug.
            }
            onPdf(key)
        }
    }
}

private const val TRUSTED_ORIGIN = "https://example.com/"

private val HTML = """
    <html>
    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
    <body style="font-family: sans-serif; padding: 16px; line-height: 2.4">
      <h2>Documents</h2>
      <a href="$SINGLE_PAGE_PDF">One-page PDF (application/pdf)</a><br>
      <a href="$MULTI_PAGE_PDF">Many pages (application/octet-stream)</a>
      <h2>Made by the page</h2>
      <button onclick="asDataUrl()">Data URL + name</button><br>
      <button onclick="KompasPdf.onPdf(GENERATED, null)">Plain Base64, no name</button><br>
      <button onclick="KompasPdf.onPdf('not base64 at all', 'broken.pdf')">Not Base64</button>
      <script>
        // A 2-page PDF, as if the page had just generated it.
        const GENERATED = "$GENERATED_PDF_BASE64";

        // The usual web code: a Blob (e.g. from fetch or a PDF library) read as a data: URL.
        function asDataUrl() {
          const bytes = Uint8Array.from(atob(GENERATED), c => c.charCodeAt(0));
          const reader = new FileReader();
          reader.onload = () => KompasPdf.onPdf(reader.result, "Raport wygenerowany.pdf");
          reader.readAsDataURL(new Blob([bytes], { type: "application/pdf" }));
        }
      </script>
    </body>
    </html>
""".trimIndent()

private const val GENERATED_PDF_BASE64 = "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUiA1IDAgUl0gL0NvdW50IDIgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA1OTUgODQyXSAvQ29udGVudHMgNCAwIFIgL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgNyAwIFIgPj4gPj4gPj4KZW5kb2JqCjQgMCBvYmoKPDwgL0xlbmd0aCA2MSA+PgpzdHJlYW0KQlQgL0YxIDI4IFRmIDcyIDc2MCBUZCAoR2VuZXJhdGVkIGluIHRoZSBwYWdlIC0gcGFnZSAxKSBUaiBFVAplbmRzdHJlYW0KZW5kb2JqCjUgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA1OTUgODQyXSAvQ29udGVudHMgNiAwIFIgL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgNyAwIFIgPj4gPj4gPj4KZW5kb2JqCjYgMCBvYmoKPDwgL0xlbmd0aCA2MSA+PgpzdHJlYW0KQlQgL0YxIDI4IFRmIDcyIDc2MCBUZCAoR2VuZXJhdGVkIGluIHRoZSBwYWdlIC0gcGFnZSAyKSBUaiBFVAplbmRzdHJlYW0KZW5kb2JqCjcgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iagp4cmVmCjAgOAowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1OCAwMDAwMCBuIAowMDAwMDAwMTIxIDAwMDAwIG4gCjAwMDAwMDAyNDcgMDAwMDAgbiAKMDAwMDAwMDM1OCAwMDAwMCBuIAowMDAwMDAwNDg0IDAwMDAwIG4gCjAwMDAwMDA1OTUgMDAwMDAgbiAKdHJhaWxlcgo8PCAvU2l6ZSA4IC9Sb290IDEgMCBSID4+CnN0YXJ0eHJlZgo2NjUKJSVFT0YK"
