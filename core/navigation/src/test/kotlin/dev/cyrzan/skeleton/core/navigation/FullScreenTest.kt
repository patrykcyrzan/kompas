package dev.cyrzan.skeleton.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenTest {

    @Test
    fun `entries are full screen only when declared`() {
        assertTrue(fullScreen().isFullScreen())
        assertFalse(emptyMap<String, Any>().isFullScreen())
    }

    @Test
    fun `combines with other metadata`() {
        val metadata = fullScreen() + mapOf("other" to 1)
        assertTrue(metadata.isFullScreen())
        assertEquals(1, metadata["other"])
    }
}
