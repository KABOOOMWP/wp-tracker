package ch.wastedpotential.padel.model

import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.engine.makeSnapshot
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.engine.winGame
import ch.wastedpotential.padel.match.MatchHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetScoreTest {

    @Test fun `a completed set is won by the team with more games`() {
        assertEquals(Team.YOU, SetScore(6, 4, true).winner)
        assertEquals(Team.OPP, SetScore(5, 7, true).winner)
    }

    @Test fun `a tie-break set is won 7-6 either way`() {
        assertEquals(Team.YOU, SetScore(7, 6, true).winner)
        assertEquals(Team.OPP, SetScore(6, 7, true).winner)
        assertTrue(SetScore(6, 7, true).wasTieBreak)
        assertFalse(SetScore(7, 5, true).wasTieBreak)
        assertFalse(SetScore(6, 6, false).wasTieBreak, "a tie-break still being played")
    }

    @Test fun `an unfinished set has no winner whoever leads`() {
        assertNull(SetScore(5, 3, false).winner)
        assertNull(SetScore(0, 0, false).winner)
        assertNull(SetScore(6, 6, false).winner)
    }

    @Test fun `a set is live unless said completed`() {
        assertEquals(SetScore(3, 2, false), SetScore(3, 2))
        assertFalse(SetScore(3, 2).isCompleted)
    }

    @Test fun `a completed set cannot be level`() {
        assertFailsWith<IllegalArgumentException> { SetScore(6, 6, true) }
        assertFailsWith<IllegalArgumentException> { SetScore(0, 0, true) }
    }

    @Test fun `games are read per team`() {
        val set = SetScore(6, 3, true)
        assertEquals(6, set.games(Team.YOU))
        assertEquals(3, set.games(Team.OPP))
    }

    @Test fun `only the loser of a completed set has lost it`() {
        assertTrue(SetScore(6, 4, true).lostBy(Team.OPP))
        assertFalse(SetScore(6, 4, true).lostBy(Team.YOU))
        assertFalse(SetScore(2, 5, false).lostBy(Team.YOU), "cut short or still being played")
    }

    @Test fun `after End match the leader of the cut-short set has not won it`() {
        val history = MatchHistory().apply { start(singlesConfig(), 0) }
        repeat(24) { history.score(Team.OPP) } // 0-6
        history.answerPrompt(0)
        repeat(16) { history.score(Team.YOU) } // 4-0 in the second set
        history.end(60_000)
        val sets = SummaryStats.of(history.current!!).sets
        assertEquals(listOf(SetScore(0, 6, true), SetScore(4, 0, false)), sets)
        assertEquals(listOf(Team.OPP, null), sets.map { it.winner })
    }

    @Test fun `the engine records the set winner as it completes the set`() {
        val s = makeSnapshot(youGames = 5, oppGames = 6).winGame(Team.OPP)
        assertEquals(SetScore(5, 7, true), s.match.completedSets.single())
        assertEquals(Team.OPP, s.match.completedSets.single().winner)
        assertTrue(s.awaitingCourtSideChange, "that one game ended the set")
    }
}
