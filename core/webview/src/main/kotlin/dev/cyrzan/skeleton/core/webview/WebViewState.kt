package dev.cyrzan.skeleton.core.webview

import android.os.Bundle
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** What to show. Set once per screen; a different value loads it in the same WebView. */
sealed interface WebContent {

    /**
     * @param headers sent with THIS request only – not with links the user follows afterwards.
     *   A web session belongs in cookies (`CookieManager`), not in headers.
     */
    data class Url(val url: String, val headers: Map<String, String> = emptyMap()) : WebContent

    /** HTML from the app or the backend. [baseUrl] is its origin: relative links, cookies and origin checks use it. */
    data class Html(val html: String, val baseUrl: String) : WebContent
}

/** A link the WebView can't show (PDF, ZIP…) – what `DownloadListener` reports. */
data class WebDownload(val url: String, val contentDisposition: String?, val mimeType: String?)

/**
 * Everything the screen around a [KompasWebView] needs: the page's state, its title, whether Back
 * goes through the page's history – and the two actions a screen offers, [reload] and [goBack].
 *
 * Survives leaving the screen (Nav3 drops it from composition when another screen is pushed) and
 * process death: the page's history is saved, the WebView reloads the page it was on.
 */
@Stable
class WebViewState internal constructor(private var savedHistory: Bundle?) {

    private var pageLoad: PageLoad by mutableStateOf(PageLoad())

    val pageState: WebPageState
        get() = pageLoad.state

    /** The page's `<title>`, `null` until it has one. */
    var title: String? by mutableStateOf(null)
        private set

    var canGoBack: Boolean by mutableStateOf(false)
        private set

    /** Bumped to replace a WebView whose renderer is gone – a new one is created under a new key. */
    internal var generation: Int by mutableIntStateOf(0)
        private set

    internal var loadedContent: WebContent? = null
    private var webView: WebView? = null

    /** Try again – also after a renderer crash, with a new WebView. */
    fun reload() {
        if (pageState == RENDERER_GONE) {
            loadedContent = null
            pageLoad = PageLoad()
            generation++
        } else {
            webView?.reload()
        }
    }

    fun goBack() {
        webView?.goBack()
    }

    internal fun onEvent(event: PageEvent) {
        pageLoad = pageLoad.reduce(event)
        // WebView's own error page has a title ("Web page not available") – not the page's.
        if (pageState is WebPageState.Error) title = null
    }

    internal fun onHistoryChanged(view: WebView) {
        canGoBack = view.canGoBack()
    }

    internal fun onTitle(view: WebView, title: String?) {
        // Without a <title>, WebView reports the URL – not something to show in a toolbar.
        if (pageState is WebPageState.Error) return
        this.title = title?.takeIf { it.isNotBlank() && it != view.url && !it.startsWith("data:") }
    }

    /** @return `true` when the history was restored – the WebView is already loading the page it was on. */
    internal fun attach(view: WebView): Boolean {
        webView = view
        val history = savedHistory ?: return false
        savedHistory = null
        return view.restoreState(history) != null
    }

    internal fun detach(view: WebView) {
        // A crashed WebView has nothing worth restoring – retry starts from the content again.
        savedHistory = if (pageState == RENDERER_GONE) null else Bundle().also(view::saveState)
        webView = null
        canGoBack = false
    }

    internal fun onResume() {
        webView?.onResume()
    }

    internal fun onPause() {
        webView?.onPause()
    }

    internal val isRendererGone: Boolean
        get() = pageState == RENDERER_GONE

    private fun saveHistory(): Bundle? = webView?.let { view -> Bundle().also(view::saveState) } ?: savedHistory

    internal companion object {
        private val RENDERER_GONE = WebPageState.Error(WebPageError.RendererGone)

        val Saver = Saver<WebViewState, Bundle>(
            save = { it.saveHistory() },
            restore = { WebViewState(it) },
        )
    }
}

@Composable
fun rememberWebViewState(): WebViewState = rememberSaveable(saver = WebViewState.Saver) { WebViewState(savedHistory = null) }
