package dev.cyrzan.skeleton.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EntryIdTest {

    private data class Item(val id: String) : NavKey

    @Test
    fun `equal keys on one stack are different entries`() {
        val stack = listOf(Item("7"), Item("8"), Item("7"))

        val ids = stack.mapIndexed { index, key -> entryId("tab0", index, key) }

        assertEquals(3, ids.toSet().size)
    }

    @Test
    fun `entry keeps its identity while the stack changes above it`() {
        val before = entryId("tab0", 1, Item("7"))
        val after = entryId("tab0", 1, Item("7")) // [Home, Item7] → push → pop

        assertEquals(before, after)
    }

    @Test
    fun `another screen at a reused position is a new entry`() {
        assertNotEquals(entryId("tab0", 1, Item("7")), entryId("tab0", 1, Item("8")))
    }

    @Test
    fun `same key on two stacks is two entries`() {
        assertNotEquals(entryId("tab0", 1, Item("7")), entryId("tab1", 1, Item("7")))
    }
}
