package dev.cyrzan.skeleton.core.webview

import org.junit.Assert.assertEquals
import org.junit.Test

class WebPageStateTest {

    private fun run(vararg events: PageEvent): WebPageState = events.fold(PageLoad()) { load, event -> load.reduce(event) }.state

    @Test
    fun `a page that loads ends Loaded`() {
        assertEquals(WebPageState.Loaded, run(PageEvent.Started, PageEvent.Progress(60), PageEvent.Finished))
    }

    @Test
    fun `finishing after a failure keeps the error`() {
        val error = WebPageError.Network(code = -2, description = "net::ERR_NAME_NOT_RESOLVED")
        assertEquals(WebPageState.Error(error), run(PageEvent.Started, PageEvent.Failed(error), PageEvent.Finished))
    }

    /** The order WebView really uses for a 404: the error comes with the headers, before onPageStarted. */
    @Test
    fun `an HTTP error before its page starts is not lost`() {
        val state = run(PageEvent.Failed(WebPageError.Http(404)), PageEvent.Started, PageEvent.Finished)
        assertEquals(WebPageState.Error(WebPageError.Http(404)), state)
    }

    @Test
    fun `an early HTTP error while the previous page is shown`() {
        val state = run(PageEvent.Started, PageEvent.Finished, PageEvent.Failed(WebPageError.Http(500)), PageEvent.Started, PageEvent.Finished)
        assertEquals(WebPageState.Error(WebPageError.Http(500)), state)
    }

    @Test
    fun `a new load clears the error`() {
        val state = run(PageEvent.Failed(WebPageError.Http(500)), PageEvent.Started, PageEvent.Finished, PageEvent.Started)
        assertEquals(WebPageState.Loading(), state)
    }

    @Test
    fun `reload after an error can end Loaded`() {
        val state = run(
            PageEvent.Failed(WebPageError.Http(503)), PageEvent.Started, PageEvent.Finished,
            PageEvent.Started, PageEvent.Finished,
        )
        assertEquals(WebPageState.Loaded, state)
    }

    @Test
    fun `the first error of a load is the one shown`() {
        assertEquals(
            WebPageState.Error(WebPageError.Ssl),
            run(PageEvent.Started, PageEvent.Failed(WebPageError.Ssl), PageEvent.Failed(WebPageError.Network(-1, null))),
        )
        assertEquals(
            WebPageState.Error(WebPageError.Ssl),
            run(PageEvent.Failed(WebPageError.Ssl), PageEvent.Failed(WebPageError.Network(-1, null)), PageEvent.Started),
        )
    }

    @Test
    fun `a renderer crash shows at once and overrides an earlier error`() {
        assertEquals(WebPageState.Error(WebPageError.RendererGone), run(PageEvent.Failed(WebPageError.RendererGone)))
        val state = run(PageEvent.Started, PageEvent.Failed(WebPageError.Http(404)), PageEvent.Failed(WebPageError.RendererGone))
        assertEquals(WebPageState.Error(WebPageError.RendererGone), state)
    }

    @Test
    fun `progress only moves a loading page`() {
        assertEquals(WebPageState.Loading(40), run(PageEvent.Started, PageEvent.Progress(40)))
        assertEquals(WebPageState.Loaded, run(PageEvent.Started, PageEvent.Finished, PageEvent.Progress(100)))
        assertEquals(WebPageState.Loading(100), run(PageEvent.Started, PageEvent.Progress(250)))
    }
}
