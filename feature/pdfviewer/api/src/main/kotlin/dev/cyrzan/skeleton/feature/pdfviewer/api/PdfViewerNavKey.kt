package dev.cyrzan.skeleton.feature.pdfviewer.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Shows a PDF inside the app, with Share and Save.
 *
 * Not a [dev.cyrzan.skeleton.core.deeplink.Destination]: a link must not make the app download whatever URL it carries.
 *
 * @param fileName title and name for Share/Save; `null` – from the response's `Content-Disposition`, then the URL.
 */
@Serializable
data class PdfViewerNavKey(val source: PdfSource, val fileName: String? = null) : NavKey {

    /** A PDF at [url]. */
    constructor(url: String, fileName: String? = null) : this(PdfSource.Remote(url), fileName)

    companion object {
        /**
         * For a WebView `DownloadListener`: the viewer's key when the download is a PDF, else `null`.
         *
         * ```
         * webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
         *     PdfViewerNavKey.fromDownload(url, contentDisposition, mimeType)?.let(navigator::navigate)
         * }
         * ```
         */
        fun fromDownload(url: String, contentDisposition: String?, mimeType: String?): PdfViewerNavKey? {
            val headerName = PdfFileName.fromContentDisposition(contentDisposition)
            val isPdf = mimeType?.substringBefore(';')?.trim().equals(PDF_MIME_TYPE, ignoreCase = true) ||
                headerName?.endsWith(".pdf", ignoreCase = true) == true ||
                PdfFileName.fromUrl(url)?.endsWith(".pdf", ignoreCase = true) == true
            return if (isPdf) PdfViewerNavKey(url, PdfFileName.from(url, contentDisposition)) else null
        }

        const val PDF_MIME_TYPE = "application/pdf"
    }
}
