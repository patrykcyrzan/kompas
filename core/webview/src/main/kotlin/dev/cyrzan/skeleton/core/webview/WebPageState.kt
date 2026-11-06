package dev.cyrzan.skeleton.core.webview

/**
 * What the screen shows around the page. One value instead of `hasError` + `dataLoaded` flags,
 * so "loaded and failed at the same time" can't be expressed.
 */
sealed interface WebPageState {

    /** @param progress 0–100, from `WebChromeClient.onProgressChanged`. */
    data class Loading(val progress: Int = 0) : WebPageState

    data object Loaded : WebPageState

    data class Error(val error: WebPageError) : WebPageState
}

/** Only errors of the page itself (main frame) – a failed image or analytics script is not one. */
sealed interface WebPageError {

    /** No connection, unknown host, timeout… `code` is a `WebViewClient.ERROR_*`. */
    data class Network(val code: Int, val description: String?) : WebPageError

    /** The server answered the page with 4xx/5xx. */
    data class Http(val statusCode: Int) : WebPageError

    /** Invalid certificate. The load is always cancelled – never `proceed()`. */
    data object Ssl : WebPageError

    /** The renderer crashed or was killed to free memory. The WebView is gone, retry creates a new one. */
    data object RendererGone : WebPageError
}

/** The WebView callbacks that move [WebPageState], in the order WebView calls them. */
internal sealed interface PageEvent {
    data object Started : PageEvent
    data class Progress(val progress: Int) : PageEvent
    data class Failed(val error: WebPageError) : PageEvent
    data object Finished : PageEvent
}

/**
 * The whole page-load logic, free of Android so it is unit-tested. [state] is what the screen shows.
 *
 * - WebView calls `onPageFinished` after a failed load too (it shows its own error page), so Finished
 *   keeps an Error – that's what a `hasError` flag usually guards.
 * - Every load starts with Started, which clears the previous page's error: nothing to `reset()` by hand.
 * - But an HTTP error comes with the response headers – BEFORE `onPageStarted` of its own page. Clearing
 *   on Started would lose it (a 404 page would end Loaded), so an error between loads waits for the next Started.
 * - One failure often comes with a second, vaguer one (an SSL error, then a generic network error) –
 *   the first says what happened, so it stays. Only a renderer crash overrides it: the WebView is gone.
 */
internal data class PageLoad(
    val state: WebPageState = WebPageState.Loading(),
    /** Between onPageStarted and onPageFinished. */
    private val started: Boolean = false,
    /** An error that arrived before its page started. */
    private val earlyError: WebPageError? = null,
) {

    fun reduce(event: PageEvent): PageLoad = when (event) {
        PageEvent.Started -> PageLoad(state = earlyError?.let(WebPageState::Error) ?: WebPageState.Loading(), started = true)
        is PageEvent.Progress ->
            if (state is WebPageState.Loading) copy(state = WebPageState.Loading(event.progress.coerceIn(0, 100))) else this
        is PageEvent.Failed -> when {
            event.error == WebPageError.RendererGone -> copy(state = WebPageState.Error(event.error))
            !started -> copy(earlyError = earlyError ?: event.error)
            state is WebPageState.Error -> this
            else -> copy(state = WebPageState.Error(event.error))
        }
        PageEvent.Finished -> PageLoad(state = if (state is WebPageState.Error) state else WebPageState.Loaded)
    }
}
