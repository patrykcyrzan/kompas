package dev.cyrzan.skeleton.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenTest {

    private data object TestKey : NavKey

    @Test
    fun `entries are full screen only when declared`() {
        assertTrue(fullScreen().isFullScreen())
        assertFalse(emptyMap<String, Any>().isFullScreen())
    }

    @Test
    fun `combines with other metadata`() {
        val metadata = fullScreen() + sharedViewModelScope(TestKey::class)
        assertTrue(metadata.isFullScreen())
        assertEquals(TestKey::class, metadata.sharedScopeFirst())
        assertFalse(sharedViewModelScope(TestKey::class).isFullScreen())
    }
}
