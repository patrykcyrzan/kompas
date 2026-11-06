package dev.cyrzan.skeleton.core.webview

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * A WebView with the app's defaults. The screen around it reads [state] (Loading / Loaded / Error,
 * title, Back) and draws its own progress and error UI.
 *
 * @param urlHandlers the feature's rules for followed links, asked in order – see [UrlHandler].
 *   Links no rule takes: http(s) loads here, `tel:`/`mailto:`/`sms:`/`geo:` open in their app, anything else is dropped.
 * @param onDownload a link the WebView can't show, e.g. a PDF. `null` – ignored.
 * @param configure extra setup of the WebView, e.g. `addJavascriptInterface`. Runs once per WebView, before the first load.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KompasWebView(
    content: WebContent,
    state: WebViewState,
    modifier: Modifier = Modifier,
    urlHandlers: List<UrlHandler> = emptyList(),
    onDownload: ((WebDownload) -> Unit)? = null,
    javaScriptEnabled: Boolean = true,
    configure: (WebView) -> Unit = {},
) {
    BackHandler(enabled = state.canGoBack) { state.goBack() }

    // The dead WebView leaves the composition (onRelease destroys it); state.reload() creates a new one.
    if (state.isRendererGone) return

    // The listener is set once per WebView – it must call the latest lambda, not the first one.
    val currentOnDownload by rememberUpdatedState(onDownload)

    key(state.generation) {
        val client = remember { KompasWebViewClient(onEvent = state::onEvent, onHistoryChanged = state::onHistoryChanged) }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    settings.applyDefaults(javaScriptEnabled)
                    webViewClient = client
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) = state.onEvent(PageEvent.Progress(newProgress))
                        override fun onReceivedTitle(view: WebView, title: String?) = state.onTitle(view, title)
                    }
                    setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                        currentOnDownload?.invoke(WebDownload(url, contentDisposition, mimeType))
                    }
                    configure(this)
                    // Back from another screen or after process death: the page it was on, with its history.
                    if (state.attach(this)) state.loadedContent = content
                }
            },
            update = { view ->
                client.urlHandlers = urlHandlers
                if (state.loadedContent != content) {
                    state.loadedContent = content
                    view.load(content)
                }
            },
            onRelease = { view ->
                state.detach(view)
                view.destroy()
            },
        )
    }

    // A WebView in the background still runs animations and media; paused with the screen.
    LifecycleResumeEffect(state.generation) {
        state.onResume()
        onPauseOrDispose { state.onPause() }
    }
}

private fun WebView.load(content: WebContent) = when (content) {
    is WebContent.Url -> loadUrl(content.url, content.headers)
    is WebContent.Html -> loadDataWithBaseURL(content.baseUrl, content.html, "text/html", "utf-8", content.baseUrl)
}

private fun WebSettings.applyDefaults(javaScriptEnabled: Boolean) {
    this.javaScriptEnabled = javaScriptEnabled
    // localStorage / sessionStorage – most web apps break without it.
    domStorageEnabled = true
    // The page must not read the app's files or content providers.
    allowFileAccess = false
    allowContentAccess = false
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    safeBrowsingEnabled = true
    // target="_blank" and window.open() open in this WebView – a rule can send them outside instead.
    setSupportMultipleWindows(false)
}
