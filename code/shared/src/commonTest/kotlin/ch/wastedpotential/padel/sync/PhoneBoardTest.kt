package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.match.AppScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneBoardTest {

    private fun sample(name: String, revision: Long, epoch: Long = 1) =
        SampleBoards.make(name)!!.copy(epoch = epoch, revision = revision).encode()

    @Test fun `starts empty`() = assertNull(PhoneBoard().board)

    @Test fun `shows a received board`() {
        val phone = PhoneBoard()
        assertTrue(phone.receive(sample(SampleBoards.MATCH, 10)))
        assertEquals("40", phone.board!!.match!!.youScore)
    }

    @Test fun `ignores an older board`() {
        val phone = PhoneBoard()
        phone.receive(sample(SampleBoards.SUMMARY, 20))
        assertFalse(phone.receive(sample(SampleBoards.MATCH, 10)))
        assertEquals(AppScreen.SUMMARY, phone.board!!.screen)
    }

    @Test fun `shows a resend of the same board`() {
        val phone = PhoneBoard()
        phone.receive(sample(SampleBoards.SUMMARY, 20))
        assertTrue(phone.receive(sample(SampleBoards.MATCH, 20)))
        assertEquals(AppScreen.MATCH, phone.board!!.screen)
    }

    @Test fun `a reinstalled watch is shown although its revisions start over`() {
        val phone = PhoneBoard()
        phone.receive(sample(SampleBoards.SUMMARY, 20, epoch = 1))
        assertTrue(phone.receive(sample(SampleBoards.MATCH, 1, epoch = 2)))
        assertEquals(AppScreen.MATCH, phone.board!!.screen)
        assertFalse(phone.receive(sample(SampleBoards.SUMMARY, 0, epoch = 2)), "ordered by revision within the new epoch")
        assertEquals(AppScreen.MATCH, phone.board!!.screen)
    }

    @Test fun `ignores garbage`() {
        val phone = PhoneBoard()
        assertFalse(phone.receive("nope"))
        assertNull(phone.board)
    }

    @Test fun `waiting until a board arrives`() {
        val phone = PhoneBoard()
        assertNull(phone.match)
        assertNull(phone.summary)
        assertTrue(phone.showsNameEditor)
    }

    @Test fun `a match board draws the match with the name editor`() {
        val phone = PhoneBoard().apply { receive(sample(SampleBoards.MATCH, 10)) }
        assertEquals("40", phone.match!!.youScore)
        assertNull(phone.summary)
        assertTrue(phone.showsNameEditor)
    }

    @Test fun `a summary board hides the name editor`() {
        val phone = PhoneBoard().apply { receive(sample(SampleBoards.SUMMARY, 10)) }
        assertNull(phone.match)
        assertNotNull(phone.summary)
        assertFalse(phone.showsNameEditor)
    }

    @Test fun `a setup board waits`() {
        val phone = PhoneBoard().apply { receive(Scoreboard(AppScreen.SETUP, epoch = 1, revision = 10).encode()) }
        assertEquals(AppScreen.SETUP, phone.board!!.screen)
        assertNull(phone.match)
        assertNull(phone.summary)
        assertTrue(phone.showsNameEditor)
    }
}
