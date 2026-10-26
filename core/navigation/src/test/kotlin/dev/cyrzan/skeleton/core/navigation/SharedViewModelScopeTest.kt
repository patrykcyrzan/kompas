package dev.cyrzan.skeleton.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedViewModelScopeTest {

    private data class Step1(val form: String) : NavKey
    private data object Step2 : NavKey
    private data object Step3 : NavKey
    private data object Terms : NavKey
    private data object Home : NavKey

    private val firstStepOf = { key: NavKey -> if (key is Step1 || key == Step2 || key == Step3) Step1::class else null }

    @Test
    fun `steps share the scope of the first step below them`() {
        val stack = listOf(Home, Step1("a"), Step2, Terms, Step3)

        val scopes = resolveSharedScopes("flow", stack, stack.map(firstStepOf))

        val first = SharedScopeRef(entryId("flow", 1, Step1("a")), Step1("a"))
        assertEquals(listOf(null, first, first, null, first), scopes)
    }

    @Test
    fun `a second form on the same stack gets its own scope`() {
        val stack = listOf(Step1("a"), Step2, Step1("b"), Step2)

        val scopes = resolveSharedScopes("flow", stack, stack.map(firstStepOf))

        assertEquals(scopes[0], scopes[1])
        assertEquals(SharedScopeRef(entryId("flow", 2, Step1("b")), Step1("b")), scopes[3])
    }

    @Test
    fun `entries without metadata have no scope`() {
        assertNull(resolveSharedScopes("tab0", listOf(Home), listOf(null)).single())
    }

    @Test(expected = IllegalStateException::class)
    fun `a step without its first step below is a programming error`() {
        resolveSharedScopes("flow", listOf(Home, Step2), listOf(Home, Step2).map(firstStepOf))
    }
}
