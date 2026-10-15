package ch.wastedpotential.padel.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The bookkeeping behind DataLayerChannel.close: no listener outlives the channel. */
class SubscriptionsTest {

    private val removed = mutableListOf<String>()
    private val subscriptions = Subscriptions(removed::add)

    @Test fun `close removes every listener once`() {
        assertTrue(subscriptions.add("a"))
        assertTrue(subscriptions.add("b"))
        subscriptions.close()
        subscriptions.close()
        assertEquals(listOf("a", "b"), removed)
        assertFalse(subscriptions.isOpen)
    }

    @Test fun `a listener added after close is refused and never removed`() {
        subscriptions.close()
        assertFalse(subscriptions.add("late"))
        subscriptions.close()
        assertTrue(removed.isEmpty())
    }
}
