package dev.cyrzan.skeleton.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenNameTest {

    @Test
    fun `entries have a screen name only when declared`() {
        assertEquals("home", screenName("home").screenName())
        assertNull(emptyMap<String, Any>().screenName())
    }

    @Test
    fun `combines with full screen`() {
        val metadata = fullScreen() + screenName("login")
        assertTrue(metadata.isFullScreen())
        assertEquals("login", metadata.screenName())
    }
}
