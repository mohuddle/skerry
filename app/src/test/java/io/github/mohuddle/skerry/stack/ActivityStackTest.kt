package io.github.mohuddle.skerry.stack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStackTest {

    @Test
    fun keepsNewestFirstAndDropsPastTheCap() {
        val stack = ActivityStack(cap = 5)
        repeat(6) { index -> stack.offer(note("k$index", "Title $index", index.toLong())) }

        assertEquals(listOf("Title 5", "Title 4", "Title 3", "Title 2", "Title 1"), stack.state().items.map { it.title })
        assertEquals("Title 5", stack.state().current?.title)
        assertFalse(stack.state().items.any { it.title == "Title 0" })
    }

    @Test
    fun removeDropsTheKeyAndFollowsThePreviousCurrent() {
        val stack = ActivityStack()
        stack.offer(note("a", "A"))
        stack.offer(note("b", "B"))
        stack.cycle()

        stack.remove("b")

        assertEquals("A", stack.state().current?.title)
        assertEquals(listOf("A"), stack.state().items.map { it.title })
    }

    @Test
    fun replaceKeepsTheCurrentKeyAndAChargingRow() {
        val stack = ActivityStack()
        stack.offer(note("a", "A"))
        stack.offer(note("b", "B"))
        stack.cycle()
        stack.setCharging(charging())

        stack.replaceNotifications(listOf(note("b", "B2"), note("a", "A2"), note("c", "C")))

        assertEquals(listOf("B2", "A2", "C", "Charging"), stack.state().items.map { it.title })
        assertEquals("A2", stack.state().current?.title)
    }

    @Test
    fun cycleWrapsAndCollapses() {
        val stack = ActivityStack()
        stack.offer(note("a", "A"))
        stack.offer(note("b", "B"))
        stack.toggleExpanded()

        stack.cycle()
        assertEquals("A", stack.state().current?.title)
        assertFalse(stack.state().expanded)

        stack.cycle()
        assertEquals("B", stack.state().current?.title)
    }

    @Test
    fun centerTogglesExpandedOnlyWhenSomethingIsCurrent() {
        val stack = ActivityStack()
        stack.toggleExpanded()
        assertFalse(stack.state().expanded)

        stack.offer(note("a", "A"))
        stack.toggleExpanded()
        assertTrue(stack.state().expanded)
        stack.toggleExpanded()
        assertFalse(stack.state().expanded)
    }

    @Test
    fun chargingLeavesWhenClearedAndDoesNotConsumeTheNewestSlot() {
        val stack = ActivityStack(cap = 2)
        stack.setCharging(charging())
        stack.offer(note("a", "A"))
        stack.offer(note("b", "B"))

        assertEquals(listOf("B", "Charging"), stack.state().items.map { it.title })

        stack.setCharging(null)
        assertNull(stack.state().items.find { it.kind == ItemKind.CHARGING })
        assertEquals(listOf("B"), stack.state().items.map { it.title })
    }

    private fun note(key: String, title: String, postedAt: Long = 1L) = LiveItem(
        key = key,
        packageName = "app.example",
        title = title,
        text = "body",
        postedAt = postedAt,
    )

    private fun charging() = LiveItem(
        key = "skerry:charging",
        packageName = null,
        title = "Charging",
        text = "",
        postedAt = 0L,
        kind = ItemKind.CHARGING,
    )
}
