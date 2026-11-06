package dev.cyrzan.skeleton.feature.pdfviewer.sandbox

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import dev.cyrzan.skeleton.sandbox.Sandbox
import dev.cyrzan.skeleton.sandbox.Scenario

@Module
@InstallIn(SingletonComponent::class)
internal object PdfViewerSandboxModule {

    @Provides
    fun provideSandbox() = Sandbox(
        name = "PDF viewer",
        scenarios = listOf(
            // The real path: a link in a WebView, caught by its DownloadListener.
            Scenario("From a WebView link", WebViewHarnessNavKey),
            Scenario("One page", PdfViewerNavKey(SINGLE_PAGE_PDF)),
            // Served as application/octet-stream, without Content-Disposition – the name comes from the URL.
            Scenario("Many pages", PdfViewerNavKey(MULTI_PAGE_PDF)),
            Scenario("Name from the caller", PdfViewerNavKey(SINGLE_PAGE_PDF, fileName = "Wyciąg październik 2026.pdf")),
            Scenario("Download fails (404)", PdfViewerNavKey("https://example.com/missing.pdf")),
            // An HTML page where a PDF was expected, like a login page after the session expired.
            Scenario("Not a PDF", PdfViewerNavKey("https://example.com/", fileName = "statement.pdf")),
        ),
    )
}

internal const val SINGLE_PAGE_PDF = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
internal const val MULTI_PAGE_PDF = "https://raw.githubusercontent.com/mozilla/pdf.js/master/web/compressed.tracemonkey-pldi-09.pdf"
