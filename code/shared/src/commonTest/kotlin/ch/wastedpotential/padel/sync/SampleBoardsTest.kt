package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.match.AppScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SampleBoardsTest {

    @Test fun `match sample is 40-15`() {
        val board = SampleBoards.make(SampleBoards.MATCH)!!
        assertEquals(AppScreen.MATCH, board.screen)
        val match = assertNotNull(board.match)
        assertEquals("40", match.youScore)
        assertEquals("15", match.oppScore)
    }

    @Test fun `summary sample is a 75 minute win`() {
        val stats = SampleBoards.make(SampleBoards.SUMMARY)!!.summary!!
        assertEquals(UiText.YOU_WIN, stats.resultLabel)
        assertEquals("01:15", stats.duration)
    }

    @Test fun `unknown sample`() = assertNull(SampleBoards.make("nope"))
}
