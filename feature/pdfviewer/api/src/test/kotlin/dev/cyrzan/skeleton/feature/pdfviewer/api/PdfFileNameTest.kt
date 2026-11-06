package dev.cyrzan.skeleton.feature.pdfviewer.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfFileNameTest {

    private val url = "https://bank.example.com/docs/statement?id=42"

    @Test
    fun `quoted filename`() {
        assertEquals("statement_10.pdf", PdfFileName.from(url, """attachment; filename="statement_10.pdf""""))
    }

    @Test
    fun `unquoted filename`() {
        assertEquals("statement.pdf", PdfFileName.from(url, "inline; filename=statement.pdf"))
    }

    @Test
    fun `filename star wins and keeps non-ASCII letters`() {
        assertEquals(
            "wyciąg październik.pdf",
            PdfFileName.from(url, """attachment; filename="wyciag.pdf"; filename*=UTF-8''wyci%C4%85g%20pa%C5%BAdziernik.pdf"""),
        )
    }

    @Test
    fun `plus in filename star is literal`() {
        assertEquals("a+b.pdf", PdfFileName.from(url, "attachment; filename*=UTF-8''a+b.pdf"))
    }

    @Test
    fun `escaped quotes in a quoted filename`() {
        assertEquals("say _hi_.pdf", PdfFileName.from(url, """attachment; filename="say \"hi\".pdf""""))
    }

    @Test
    fun `falls back to the URL's last segment, decoded`() {
        assertEquals("umowa kredytowa.pdf", PdfFileName.from("https://example.com/files/umowa%20kredytowa.pdf?v=2", null))
    }

    @Test
    fun `adds the extension`() {
        assertEquals("statement.pdf", PdfFileName.from("https://example.com/docs/statement", null))
    }

    @Test
    fun `default name when nothing usable`() {
        assertEquals("document.pdf", PdfFileName.from("https://example.com/", "attachment"))
    }

    @Test
    fun `strips paths from the server's name`() {
        assertEquals("passwd.pdf", PdfFileName.from(url, """attachment; filename="../../etc/passwd""""))
    }

    @Test
    fun `no name without a header`() {
        assertNull(PdfFileName.fromContentDisposition(null))
        assertNull(PdfFileName.fromContentDisposition("attachment"))
    }

    @Test
    fun `download is a PDF by mime type, header name or URL`() {
        assertEquals("statement.pdf", PdfViewerNavKey.fromDownload(url, null, "application/pdf")?.fileName)
        assertEquals("x.pdf", PdfViewerNavKey.fromDownload(url, "attachment; filename=x.pdf", "application/octet-stream")?.fileName)
        assertEquals("y.pdf", PdfViewerNavKey.fromDownload("https://example.com/y.pdf", null, null)?.fileName)
        assertNull(PdfViewerNavKey.fromDownload("https://example.com/a.zip", null, "application/zip"))
    }
}
