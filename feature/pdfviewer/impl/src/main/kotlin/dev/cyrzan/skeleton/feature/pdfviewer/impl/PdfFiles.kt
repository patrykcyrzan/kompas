package dev.cyrzan.skeleton.feature.pdfviewer.impl

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfFileName
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfSource
import dev.cyrzan.skeleton.feature.pdfviewer.api.PdfViewerNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Downloaded PDFs live in the app's cache (`cache/pdf/<id>/<name>.pdf`), never in shared storage: only the
 * viewer reads them, Share hands out a temporary content:// URI, and Save copies to where the user chose.
 * Each viewer deletes its own file when it closes; leftovers (process killed meanwhile) go on the next download or import.
 */
internal class PdfFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val root = File(context.cacheDir, ROOT_DIR)

    /** The PDF behind [key] as a file of this viewer: downloaded, or the one [importBase64] made. */
    suspend fun fetch(key: PdfViewerNavKey): File = when (val source = key.source) {
        is PdfSource.Remote -> download(source.url, key.fileName)
        is PdfSource.Local -> withContext(Dispatchers.IO) {
            // Only our own files – the viewer deletes what it showed.
            val file = File(source.path).canonicalFile
            if (file.parentFile?.parentFile != root.canonicalFile) throw IOException("Not a viewer file: $file")
            if (!file.isFile) throw FileNotFoundException(file.path)
            file
        }
    }

    /** [base64] – plain or a `data:` URL. */
    suspend fun importBase64(base64: String, fileName: String?): File = withContext(Dispatchers.IO) {
        deleteStale()
        writeNew(PdfFileName.sanitize(fileName)) { out -> decodeBase64(base64, out) }
    }

    private suspend fun download(url: String, fileName: String?): File = withContext(Dispatchers.IO) {
        deleteStale()
        val request = Request.Builder()
            .url(url)
            // The PDF is usually behind the same session as the page that linked it.
            .apply { webViewCookies(url)?.let { header("Cookie", it) } }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val name = fileName ?: PdfFileName.from(url, response.header("Content-Disposition"))
            writeNew(name) { out -> response.body.byteStream().copyTo(out) }
        }
    }

    /** `cache/pdf/<random>/<name>` – its own directory, so two PDFs with the same name don't collide. */
    private inline fun writeNew(name: String, write: (OutputStream) -> Unit): File {
        val file = File(File(root, UUID.randomUUID().toString()).apply { mkdirs() }, name)
        try {
            file.outputStream().use(write)
        } catch (e: Throwable) {
            delete(file)
            throw e
        }
        return file
    }

    /** A ready-to-start share Intent; the receiving app gets read access to this one file only. */
    fun shareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.pdfviewer.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(PdfViewerNavKey.PDF_MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Name and preview in the share sheet.
        send.clipData = ClipData.newRawUri(file.name, uri)
        return Intent.createChooser(send, null)
    }

    /** Copies [file] to [target] – a document the user created in the system's "Save as" picker. */
    suspend fun saveTo(file: File, target: Uri) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(target) ?: throw IOException("Cannot write $target")
        output.use { out -> file.inputStream().use { it.copyTo(out) } }
    }

    /** Deletes [file] together with its own directory – only if it is one of the viewer's files. */
    fun delete(file: File) {
        val dir = file.canonicalFile.parentFile ?: return
        if (dir.parentFile == root.canonicalFile) dir.deleteRecursively()
    }

    private fun deleteStale() {
        val before = System.currentTimeMillis() - STALE_AFTER.inWholeMilliseconds
        root.listFiles()?.filter { it.lastModified() < before }?.forEach { it.deleteRecursively() }
    }

    /** No WebView installed (or being updated) – then the download goes without cookies. */
    private fun webViewCookies(url: String): String? = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()

    private companion object {
        /** Keep in sync with `res/xml/pdf_file_paths.xml`. */
        const val ROOT_DIR = "pdf"
        val STALE_AFTER = 1.days
    }
}

/** Serves `cache/pdf/` to apps the user shares a PDF with. Its own class, so it does not clash with other FileProviders. */
internal class PdfFileProvider : FileProvider(R.xml.pdf_file_paths)
