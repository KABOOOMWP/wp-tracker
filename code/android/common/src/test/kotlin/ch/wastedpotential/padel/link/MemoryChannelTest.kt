package ch.wastedpotential.padel.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The test double must behave like the Data Layer, or the view-model tests prove nothing. */
class MemoryChannelTest {

    @Test fun `delivers only to listeners of the path`() {
        val channel = MemoryChannel()
        val names = mutableListOf<String>()
        channel.listen("/names", names::add)
        channel.receive("/names", "a")
        channel.receive("/scoreboard", "b")
        assertEquals(listOf("a"), names)
    }

    @Test fun `records what was sent, newest last`() {
        val channel = MemoryChannel()
        channel.send("/names", "1")
        channel.send("/scoreboard", "2")
        channel.send("/names", "3")
        assertEquals("3", channel.lastSent("/names"))
        assertNull(channel.lastSent("/other"))
        assertEquals(3, channel.sent.size)
    }

    @Test fun `a new listener gets the current value first`() {
        val channel = MemoryChannel()
        channel.receive("/names", "old")
        channel.receive("/names", "new")
        val names = mutableListOf<String>()
        channel.listen("/names", names::add)
        assertEquals(listOf("new"), names)
    }

    @Test fun `a pair delivers what one side sends to the other, also later`() {
        val (watch, phone) = MemoryChannel.pair()
        val boards = mutableListOf<String>()
        phone.listen("/scoreboard", boards::add)
        watch.send("/scoreboard", "1")
        watch.send("/names-unrelated", "x")
        assertEquals(listOf("1"), boards)

        watch.send("/scoreboard", "2")
        val late = mutableListOf<String>()
        MemoryChannel.pair().second.listen("/scoreboard", late::add)
        assertTrue(late.isEmpty())
        phone.listen("/scoreboard", late::add)
        assertEquals(listOf("2"), late)
    }

    @Test fun `nothing is delivered after close, also to a later listener`() {
        val channel = MemoryChannel()
        val names = mutableListOf<String>()
        channel.listen("/names", names::add)
        channel.close()
        channel.receive("/names", "a")
        channel.listen("/names", names::add)
        assertTrue(names.isEmpty())
        assertTrue(channel.isClosed)
    }
}
