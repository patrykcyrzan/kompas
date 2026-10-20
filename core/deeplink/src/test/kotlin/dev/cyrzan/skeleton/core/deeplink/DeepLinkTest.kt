package dev.cyrzan.skeleton.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {

    @Test
    fun `parses web link`() {
        val link = DeepLink.parse("https://user@Example.com:443/items/42/?ref=mail&ref=other#top")!!
        assertEquals("https", link.scheme)
        assertEquals("Example.com", link.host)
        assertEquals(listOf("items", "42"), link.pathSegments)
        assertEquals(mapOf("ref" to "mail"), link.queryParameters)
    }

    @Test
    fun `parses custom scheme with and without slashes`() {
        assertEquals(DeepLink("kompas", "items", listOf("42"), emptyMap()), DeepLink.parse("KOMPAS://items/42"))
        assertEquals(DeepLink("kompas", null, listOf("items", "42"), emptyMap()), DeepLink.parse("kompas:items/42"))
    }

    @Test
    fun `decodes percent encoding as utf8 and plus only in query`() {
        val link = DeepLink.parse("https://a.b/za%C5%BC%C3%B3%C5%82%C4%87+x?q=a+b%26c")!!
        assertEquals(listOf("zażółć+x"), link.pathSegments)
        assertEquals("a b&c", link.queryParameters["q"])
    }

    @Test
    fun `rejects malformed input`() {
        assertNull(DeepLink.parse("no-scheme/items"))
        assertNull(DeepLink.parse("1http://a.b"))
        assertNull(DeepLink.parse("https://a.b/%zz"))
        assertNull(DeepLink.parse("https://a.b/%4"))
    }
}
