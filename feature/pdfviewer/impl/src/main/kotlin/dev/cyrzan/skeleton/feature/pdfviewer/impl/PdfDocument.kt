package dev.cyrzan.skeleton.feature.pdfviewer.impl

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import kotlin.math.roundToInt

/**
 * An open PDF, rendered page by page with the system [PdfRenderer] (no library, nothing added to the APK).
 *
 * [PdfRenderer] allows one open page at a time and is not thread-safe – every access holds [lock],
 * including [close], so closing the screen mid-render cannot crash.
 */
internal class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    /** Width / height of each page – lets the list reserve the space before a page is rendered. */
    val pageAspectRatios: List<Float>,
) : Closeable {

    private val lock = Any()
    private var closed = false

    val pageCount: Int get() = pageAspectRatios.size

    /** Page [index] [width] pixels wide, on white (PDF pages are transparent). */
    suspend fun renderPage(index: Int, width: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (closed) throw CancellationException("Document closed")
            renderer.openPage(index).use { page ->
                val scale = minOf(width.toFloat() / page.width, MAX_PAGE_HEIGHT.toFloat() / page.height)
                val bitmap = Bitmap.createBitmap((page.width * scale).roundToInt(), (page.height * scale).roundToInt(), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    override fun close() = synchronized(lock) {
        if (closed) return
        closed = true
        renderer.close()
        descriptor.close()
    }

    companion object {
        /** Very tall pages (receipts) are scaled down rather than allocating a huge bitmap. */
        private const val MAX_PAGE_HEIGHT = 4096

        /**
         * @throws SecurityException the PDF is password-protected
         * @throws java.io.IOException the file is not a PDF or is damaged
         */
        suspend fun open(file: File): PdfDocument = withContext(Dispatchers.IO) {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            var renderer: PdfRenderer? = null
            try {
                val opened = PdfRenderer(descriptor).also { renderer = it }
                val aspectRatios = List(opened.pageCount) { index ->
                    opened.openPage(index).use { page -> page.width.toFloat() / page.height }
                }
                PdfDocument(descriptor, opened, aspectRatios)
            } catch (e: Throwable) {
                renderer?.close()
                descriptor.close()
                throw e
            }
        }
    }
}
