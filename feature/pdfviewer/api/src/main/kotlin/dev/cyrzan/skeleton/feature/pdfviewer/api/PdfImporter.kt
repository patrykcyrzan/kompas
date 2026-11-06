package dev.cyrzan.skeleton.feature.pdfviewer.api

/**
 * Hands the viewer a PDF the app already has in memory – e.g. from a page's `@JavascriptInterface`
 * when the PDF has no URL to download (generated in the page, a `blob:` URL). Injected; bound by the feature's `impl`.
 *
 * ```
 * @JavascriptInterface
 * fun onPdf(base64: String?, filename: String?) {
 *     base64 ?: return
 *     scope.launch {   // JavaBridge thread → Main
 *         navigator.navigate(pdfImporter.fromBase64(base64, filename))
 *     }
 * }
 * ```
 */
interface PdfImporter {

    /**
     * Decodes [base64] (plain, or a `data:application/pdf;base64,…` URL) into the viewer's cache.
     * Not checked to be a PDF – the viewer shows an error for anything else.
     *
     * @param fileName as given by the page; sanitized, `document.pdf` when null or blank
     * @throws IllegalArgumentException [base64] is not Base64
     */
    suspend fun fromBase64(base64: String, fileName: String?): PdfViewerNavKey
}
