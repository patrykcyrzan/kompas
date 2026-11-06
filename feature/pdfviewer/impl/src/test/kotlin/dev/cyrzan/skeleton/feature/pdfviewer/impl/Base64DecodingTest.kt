package dev.cyrzan.skeleton.feature.pdfviewer.impl

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64

class Base64DecodingTest {

    private val pdf = "%PDF-1.4 ąę\n%%EOF".toByteArray()
    private val encoded = Base64.getEncoder().encodeToString(pdf)

    private fun decode(input: String): String =
        ByteArrayOutputStream().also { decodeBase64(input, it) }.toByteArray().decodeToString()

    @Test
    fun `plain Base64`() {
        assertEquals(pdf.decodeToString(), decode(encoded))
    }

    @Test
    fun `data URL from FileReader`() {
        assertEquals(pdf.decodeToString(), decode("data:application/pdf;base64,$encoded"))
    }

    @Test
    fun `data URL with parameters`() {
        assertEquals(pdf.decodeToString(), decode("data:application/pdf;name=x.pdf;base64,$encoded"))
    }

    @Test
    fun `line breaks and spaces are skipped`() {
        assertEquals(pdf.decodeToString(), decode(encoded.chunked(8).joinToString("\r\n ")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `data URL that is not Base64`() {
        decode("data:application/pdf,%25PDF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `not Base64`() {
        decode("this is *not* base64")
    }
}
