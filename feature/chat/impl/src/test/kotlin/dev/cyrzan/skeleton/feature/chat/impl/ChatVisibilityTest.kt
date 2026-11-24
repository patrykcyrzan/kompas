package dev.cyrzan.skeleton.feature.chat.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ChatVisibilityTest {

    private fun TestScope.visibility() = ChatVisibility(backgroundScope, showDelay = 10.seconds).also { runCurrent() }

    private fun TestScope.advanceBy(seconds: Int) {
        advanceTimeBy(seconds.seconds)
        runCurrent()
    }

    @Test
    fun `hidden until the config switches the chat on`() = runTest {
        val visibility = visibility()

        advanceBy(60)

        assertFalse(visibility.isVisible.value)
    }

    @Test
    fun `shown only after the delay`() = runTest {
        val visibility = visibility()

        visibility.setEnabled(true)
        advanceBy(9)
        assertFalse(visibility.isVisible.value)

        advanceBy(1)
        assertTrue(visibility.isVisible.value)
    }

    @Test
    fun `switched off during the delay is never shown`() = runTest {
        val visibility = visibility()

        visibility.setEnabled(true)
        advanceBy(5)
        visibility.setEnabled(false)
        advanceBy(60)

        assertFalse(visibility.isVisible.value)
    }

    @Test
    fun `switched off when shown hides it at once`() = runTest {
        val visibility = visibility()
        visibility.setEnabled(true)
        advanceBy(10)

        visibility.setEnabled(false)
        runCurrent()

        assertFalse(visibility.isVisible.value)
    }

    @Test
    fun `a repeated config does not restart the delay`() = runTest {
        val visibility = visibility()

        visibility.setEnabled(true)
        advanceBy(5)
        visibility.setEnabled(true)
        advanceBy(5)

        assertTrue(visibility.isVisible.value)
    }
}
