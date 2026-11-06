package dev.cyrzan.skeleton.core.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * The only `WebViewClient` in the app. It turns WebView callbacks into [PageEvent]s and asks the
 * feature's [UrlHandler]s about links – it holds no state of its own and knows nothing about features.
 */
internal class KompasWebViewClient(
    private val onEvent: (PageEvent) -> Unit,
    private val onHistoryChanged: (WebView) -> Unit,
) : WebViewClient() {

    /** Replaced on every recomposition, so a rule never calls a stale lambda. */
    var urlHandlers: List<UrlHandler> = emptyList()

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        // Iframes (maps, videos, payment widgets) navigate inside themselves – not the user leaving the page.
        if (!request.isForMainFrame) return false
        val url = request.url
        if (urlHandlers.any { it.handle(url) }) return true
        return when (url.scheme?.lowercase()) {
            "https", "http" -> false
            // tel:, mailto: … the WebView can't load them and would show ERR_UNKNOWN_URL_SCHEME as the page's error.
            // The rest (intent:, file:, javascript:, chrome:) is dropped.
            else -> {
                view.context.openExternally(url)
                true
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = onEvent(PageEvent.Started)

    override fun onPageFinished(view: WebView, url: String?) {
        onEvent(PageEvent.Finished)
        onHistoryChanged(view)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = onHistoryChanged(view)

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onEvent(PageEvent.Failed(WebPageError.Network(error.errorCode, error.description?.toString())))
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) onEvent(PageEvent.Failed(WebPageError.Http(errorResponse.statusCode)))
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Never proceed() – Play rejects apps that do, and it turns HTTPS off for this page.
        handler.cancel()
        // SslError doesn't say which frame; the page's own URL is the best hint.
        if (error.url == view.url) onEvent(PageEvent.Failed(WebPageError.Ssl))
    }

    /**
     * Without `true` the whole app crashes with the renderer. This WebView is unusable afterwards:
     * [KompasWebView] drops it and creates a new one on retry.
     */
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onEvent(PageEvent.Failed(WebPageError.RendererGone))
        return true
    }
}
