package dev.cyrzan.skeleton.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

class RedactionTest {

    @Test
    fun `path and query are masked`() {
        assertEquals("https://app.example.com/*/*?…", redactDeepLink("https://app.example.com/reset/abc123?email=x%40y.z"))
        assertEquals("myapp://items/*", redactDeepLink("myapp://items/42"))
        assertEquals("<malformed>", redactDeepLink("not a uri"))
    }
}
