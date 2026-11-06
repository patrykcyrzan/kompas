package dev.cyrzan.skeleton.feature.pdfviewer.impl

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64

/**
 * Decodes [base64] into [out] as a stream – the String is already in memory, the decoded bytes needn't be too.
 * Takes a plain Base64 string or a `data:…;base64,` URL (what `FileReader.readAsDataURL` gives a page);
 * line breaks and spaces are skipped.
 *
 * @throws IllegalArgumentException not Base64
 */
internal fun decodeBase64(base64: String, out: OutputStream) {
    val start = if (base64.startsWith("data:", ignoreCase = true)) {
        val comma = base64.indexOf(',')
        require(comma >= 0 && base64.substring(0, comma).endsWith(";base64", ignoreCase = true)) { "Not a Base64 data URL" }
        comma + 1
    } else {
        0
    }
    val input = Base64.getDecoder().wrap(Base64CharsStream(base64, start))
    // The decoder throws IOException for bad input mid-stream; callers expect IllegalArgumentException.
    try {
        input.copyTo(out)
    } catch (e: IOException) {
        throw IllegalArgumentException("Not Base64", e)
    }
}

/** The Base64 characters of [chars] from [start], without whitespace – read in place, not copied. */
private class Base64CharsStream(private val chars: String, start: Int) : InputStream() {
    private var index = start

    override fun read(): Int {
        while (index < chars.length) {
            val c = chars[index++]
            if (!c.isWhitespace()) return c.code
        }
        return -1
    }
}
