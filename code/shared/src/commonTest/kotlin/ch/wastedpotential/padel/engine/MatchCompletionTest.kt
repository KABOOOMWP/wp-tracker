package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The match is over exactly when a team has the sets it needs: best of 3 (2-0, 2-1) and best of 5 (3-0 to 3-2). */
class MatchCompletionTest {

    /** Wins a 6-0 set for [winner] and answers the prompts; the match must still be running before it. */
    private fun Snapshot.set(winner: Team): Snapshot {
        assertFalse(isMatchOver, "a set is played while the match is running")
        return winSetAndContinue(winner)
    }

    private fun setsWon(s: Snapshot) = s.match.setsWon(Team.YOU) to s.match.setsWon(Team.OPP)

    @Test fun `best of 3 is over at 2-0`() {
        val s = makeSnapshot(config = singlesConfig(bestOf = 3)).set(Team.YOU).set(Team.YOU)
        assertTrue(s.isMatchOver)
        assertEquals(2 to 0, setsWon(s))
    }

    @Test fun `best of 3 is over at 2-1 only`() {
        val level = makeSnapshot(config = singlesConfig(bestOf = 3)).set(Team.YOU).set(Team.OPP)
        assertEquals(1 to 1, setsWon(level))
        val s = level.set(Team.YOU)
        assertTrue(s.isMatchOver)
        assertEquals(2 to 1, setsWon(s))
    }

    @Test fun `the opponent can win too`() {
        val s = makeSnapshot(config = singlesConfig(bestOf = 3)).set(Team.OPP).set(Team.OPP)
        assertTrue(s.isMatchOver)
        assertEquals(0 to 2, setsWon(s))
    }

    @Test fun `best of 5 is over at 3-0`() {
        val s = makeSnapshot(config = singlesConfig(bestOf = 5)).set(Team.YOU).set(Team.YOU).set(Team.YOU)
        assertTrue(s.isMatchOver)
        assertEquals(3 to 0, setsWon(s))
    }

    @Test fun `best of 5 is not over at 2-0 or 2-2`() {
        val twoLove = makeSnapshot(config = singlesConfig(bestOf = 5)).set(Team.YOU).set(Team.YOU)
        assertFalse(twoLove.isMatchOver)
        val twoAll = twoLove.set(Team.OPP).set(Team.OPP)
        assertFalse(twoAll.isMatchOver)
        assertEquals(2 to 2, setsWon(twoAll))
    }

    @Test fun `best of 5 is over at 3-1 and 3-2`() {
        val threeOne = makeSnapshot(config = singlesConfig(bestOf = 5)).set(Team.YOU).set(Team.OPP).set(Team.YOU).set(Team.YOU)
        assertTrue(threeOne.isMatchOver)
        assertEquals(3 to 1, setsWon(threeOne))
        val threeTwo = makeSnapshot(
            config = singlesConfig(bestOf = 5),
        ).set(Team.YOU).set(Team.OPP).set(Team.YOU).set(Team.OPP).set(Team.YOU)
        assertTrue(threeTwo.isMatchOver)
        assertEquals(3 to 2, setsWon(threeTwo))
    }

    @Test fun `the match is over with the winning point and not before`() {
        val matchPoint = makeSnapshot(config = singlesConfig(bestOf = 3), setsWonYou = 1, youGames = 5, youPoints = 3)
        assertFalse(matchPoint.isMatchOver)
        val won = matchPoint.you()
        assertTrue(won.isMatchOver)
        assertEquals(2, won.match.completedSets.size, "no new set is started")
        assertFalse(won.awaitingCourtSideChange)
    }
}
