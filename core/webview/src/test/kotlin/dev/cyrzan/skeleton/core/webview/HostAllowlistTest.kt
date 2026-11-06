package dev.cyrzan.skeleton.core.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostAllowlistTest {

    private val allowlist = HostAllowlist(setOf("example.com"))

    @Test
    fun `the host and its subdomains are allowed`() {
        assertTrue(allowlist.allows("example.com"))
        assertTrue(allowlist.allows("www.example.com"))
        assertTrue(allowlist.allows("help.eu.example.com"))
    }

    @Test
    fun `case and a trailing dot don't matter`() {
        assertTrue(allowlist.allows("Example.COM"))
        assertTrue(allowlist.allows("example.com."))
    }

    @Test
    fun `look-alike hosts are not allowed`() {
        assertFalse(allowlist.allows("evil-example.com"))
        assertFalse(allowlist.allows("example.com.evil.net"))
        assertFalse(allowlist.allows("examplexcom"))
    }

    @Test
    fun `no host is not allowed`() {
        assertFalse(allowlist.allows(null))
        assertFalse(allowlist.allows(""))
    }
}
