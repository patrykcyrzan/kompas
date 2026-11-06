package dev.cyrzan.skeleton.feature.webpage.sandbox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.feature.webpage.api.WebPageNavKey
import dev.cyrzan.skeleton.sandbox.Sandbox
import dev.cyrzan.skeleton.sandbox.Scenario
import java.util.Base64

@Module
@InstallIn(SingletonComponent::class)
internal object WebPageSandboxModule {

    @Provides
    fun provideSandbox() = Sandbox(
        name = "Web page",
        scenarios = listOf(
            // Every kind of link the rules decide about, on one page.
            Scenario("Links", WebPageNavKey(LINKS_PAGE, title = "Links")),
            Scenario("Loads", WebPageNavKey("https://example.com/")),
            Scenario("404", WebPageNavKey("https://example.com/missing")),
            Scenario("Unknown host (like offline)", WebPageNavKey("https://kompas.invalid/")),
            Scenario("Bad certificate", WebPageNavKey("https://expired.badssl.com/")),
            // A WebView test URL: kills the renderer, like the system does to free memory.
            Scenario("Renderer crash", WebPageNavKey("chrome://crash")),
        ),
    )
}

private val LINKS_HTML = """
    <html>
    <head><meta name="viewport" content="width=device-width, initial-scale=1"><title>Links</title></head>
    <body style="font-family: sans-serif; padding: 16px; line-height: 2.4">
      <h3>Stay in the app</h3>
      <a href="https://example.com/">Trusted page</a><br>
      <a href="https://example.com/" target="_blank">Trusted, target=_blank</a>
      <h3>Leave the app</h3>
      <a href="https://example.com/?target=newTab">Trusted, but ?target=newTab</a><br>
      <a href="https://developer.android.com/">Untrusted host</a><br>
      <a href="https://example.com.evil.net/">Look-alike host</a><br>
      <a href="tel:+48123456789">Phone</a> · <a href="mailto:help@example.com">Mail</a>
      <h3>Other</h3>
      <a href="https://kompas.example.com/items/7">App link (another feature – placeholder here)</a><br>
      <a href="https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf">PDF (download – viewer placeholder here)</a><br>
      <a href="intent://scan/#Intent;scheme=zxing;end">intent:// (dropped)</a><br>
      <a href="javascript:alert('x')">javascript: (runs in the page – not a navigation)</a>
      <h3>Broken image – still Loaded</h3>
      <img src="https://example.com/missing.png" width="80" height="80" alt="missing">
    </body>
    </html>
""".trimIndent()

/** The sandbox has no server of its own – the page travels in the URL. */
private val LINKS_PAGE = "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(LINKS_HTML.toByteArray())
