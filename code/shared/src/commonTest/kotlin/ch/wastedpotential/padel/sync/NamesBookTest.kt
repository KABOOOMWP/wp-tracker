package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.platform.MemoryTextStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NamesBookTest {

    @Test fun `starts with the defaults`() {
        assertEquals(TeamNames.DEFAULT, NamesBook(MemoryTextStore()).names)
    }

    @Test fun `edit saves and returns the names to send`() {
        val store = MemoryTextStore()
        val book = NamesBook(store)
        assertEquals(TeamNames("Blue", "Opponent"), book.edit(" Blue ", ""))
        assertEquals(TeamNames("Blue", "Opponent"), NamesBook(store).names)
    }

    @Test fun `an edit that changes nothing returns null`() {
        assertNull(NamesBook(MemoryTextStore()).edit("", "  "))
    }

    @Test fun `receive applies names from the other device and ignores garbage`() {
        val book = NamesBook(MemoryTextStore())
        assertTrue(book.receive(TeamNames("Blue", "Red").encode()))
        assertFalse(book.receive(TeamNames("Blue", "Red").encode()))
        assertFalse(book.receive("nope"))
        assertEquals(TeamNames("Blue", "Red"), book.names)
    }

    @Test fun `unreadable saved names fall back to the defaults`() {
        assertEquals(TeamNames.DEFAULT, NamesBook(MemoryTextStore("garbage")).names)
    }
}
