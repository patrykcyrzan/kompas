package dev.cyrzan.skeleton.core.file

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameTest {

    @Test
    fun `keeps a normal name`() {
        assertEquals("FV 2026-10-001.pdf", FileName.sanitize("FV 2026-10-001.pdf"))
    }

    @Test
    fun `separators cannot leave the directory`() {
        assertEquals("_.._.._etc_passwd", FileName.sanitize("/../../etc/passwd"))
    }

    @Test
    fun `no hidden files`() {
        assertEquals("pdf", FileName.sanitize("...pdf"))
    }

    @Test
    fun `blank falls back`() {
        assertEquals("file", FileName.sanitize("  "))
        assertEquals("invoice", FileName.sanitize("", fallback = "invoice"))
    }
}
