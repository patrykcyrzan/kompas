package dev.cyrzan.skeleton.feature.pdfviewer.api

import kotlinx.serialization.Serializable

/** Where the viewer gets the PDF from. Only a reference – the bytes never go on the back stack (saved state is limited to ~1 MB). */
@Serializable
sealed interface PdfSource {

    /** Downloaded with the WebView's cookies for [url], so a PDF behind the web session opens too. */
    @Serializable
    data class Remote(val url: String) : PdfSource

    /** A file the viewer already holds – made by [PdfImporter]; deleted when the viewer closes. */
    @Serializable
    data class Local(val path: String) : PdfSource
}
