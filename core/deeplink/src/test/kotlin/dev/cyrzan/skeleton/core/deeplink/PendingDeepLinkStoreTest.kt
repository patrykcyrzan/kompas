package dev.cyrzan.skeleton.core.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class PendingDeepLinkStoreTest {

    private val time = TestTimeSource()
    private val store = PendingDeepLinkStore(ttl = 5.minutes, timeSource = time)

    @Test
    fun `pending link can be consumed exactly once`() {
        store.save("myapp://items/1", DeepLinkSource.External)
        assertEquals(PendingDeepLink("myapp://items/1", DeepLinkSource.External), store.consume())
        assertNull(store.consume())
    }

    @Test
    fun `expired link is dropped`() {
        store.save("myapp://items/1", DeepLinkSource.External)
        time += 5.minutes + 1.seconds
        assertNull(store.consume())
    }

    @Test
    fun `newer link replaces older one`() {
        store.save("myapp://items/1", DeepLinkSource.External)
        store.save("myapp://items/2", DeepLinkSource.InApp)
        assertEquals(PendingDeepLink("myapp://items/2", DeepLinkSource.InApp), store.consume())
    }
}
