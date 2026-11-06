package dev.cyrzan.skeleton.feature.pdfviewer.api

import java.net.URI
import java.net.URLDecoder

/**
 * The name a downloaded PDF is shown, shared and saved under – the server's name when it gives one.
 * Plain JVM on purpose (no `URLUtil`): `URLUtil.guessFileName` ignores `filename*`, which is how
 * non-ASCII names (e.g. Polish letters) arrive.
 */
object PdfFileName {

    /** `Content-Disposition` name, then the last URL segment, then `document.pdf`; always safe and ending in `.pdf`. */
    fun from(url: String, contentDisposition: String?): String =
        sanitize(fromContentDisposition(contentDisposition) ?: fromUrl(url) ?: DEFAULT_NAME)

    /** `filename*` (RFC 6266/5987, UTF-8 and other charsets) wins over `filename`. Not sanitized. */
    fun fromContentDisposition(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val extended = EXTENDED.find(header)?.let { match ->
            val (charset, value) = match.destructured
            // '+' is a literal in RFC 5987, not a space as URLDecoder assumes.
            runCatching { URLDecoder.decode(value.replace("+", "%2B"), charset.ifBlank { "UTF-8" }) }.getOrNull()
        }
        val plain = PLAIN.find(header)?.let { match ->
            val (quoted, token) = match.destructured
            quoted.replace(ESCAPE, "$1").ifEmpty { token.trim() }
        }
        return (extended ?: plain)?.takeIf { it.isNotBlank() }
    }

    /** The last, decoded path segment of [url], if it has one. Not sanitized. */
    fun fromUrl(url: String): String? =
        runCatching { URI(url).path }.getOrNull()
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }

    /** Safe for a file system, ending in `.pdf`; `document.pdf` for a missing or blank [name]. */
    fun sanitize(name: String?): String {
        val clean = name.orEmpty()
            // A path in the name must not leave the cache directory; the rest is rejected by some file systems.
            .substringAfterLast('/').substringAfterLast('\\')
            .replace(UNSAFE, "_")
            .trim().trim('.')
            .take(MAX_LENGTH)
            .ifBlank { DEFAULT_NAME }
        return if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
    }

    private const val DEFAULT_NAME = "document.pdf"
    private const val MAX_LENGTH = 120

    private val EXTENDED = Regex("""filename\*\s*=\s*([^';\s]*)'[^']*'([^;\s]+)""", RegexOption.IGNORE_CASE)
    private val PLAIN = Regex("""(?<![*\w])filename\s*=\s*(?:"((?:\\.|[^"\\])*)"|([^;]+))""", RegexOption.IGNORE_CASE)
    private val ESCAPE = Regex("""\\(.)""")
    private val UNSAFE = Regex("""[:*?"<>|\u0000-\u001F]""")
}
