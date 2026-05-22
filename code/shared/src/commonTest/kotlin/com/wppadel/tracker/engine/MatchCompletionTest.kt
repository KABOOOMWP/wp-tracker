package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests for match completion logic and undo from match-end state.
 *
 * Covers:
 *   - Best-of-3: match ends at 2-0 and 2-1 sets
 *   - Best-of-5: match ends at 3-0, 3-1, and 3-2 sets
 *   - isMatchOver becomes true exactly when the final set is won
 *   - Scoring after match ends returns the same snapshot (frozen)
 *   - Undo from match-complete state: isMatchOver → false, pill recalculates to MATCH_POINT
 *   - Stats do not increment after match is over
 */
class MatchCompletionTest {

    // Helper: win a full set 6:0 for YOU and clear all inter-set overlays
    private fun Snapshot.winSet(): Snapshot = winSetAndContinue(Team.YOU)

    // -----------------------------------------------------------------------
    // Best-of-3 completion
    // -----------------------------------------------------------------------

    @Test fun `best-of-3 2-0 sets - match over after winning 2nd set`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        assertFalse(s.isMatchOver)

        s = s.winSet()  // set 1
        assertFalse(s.isMatchOver)
        assertEquals(1, s.match.setsWonYou)

        s = s.winSet()  // set 2
        assertTrue(s.isMatchOver)
        assertEquals(2, s.match.setsWonYou)
    }

    @Test fun `best-of-3 2-1 sets - match over only after 3rd set won`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))

        s = s.winSet()            // you 1:0
        // opp wins set 2
        var s2 = s.winSetAndContinue(Team.OPP)
        assertFalse(s2.isMatchOver)
        assertEquals(1, s2.match.setsWonYou)
        assertEquals(1, s2.match.setsWonOpp)

        val s3 = s2.winSet()     // you 2:1 — match over
        assertTrue(s3.isMatchOver)
        assertEquals(2, s3.match.setsWonYou)
        assertEquals(1, s3.match.setsWonOpp)
    }

    // -----------------------------------------------------------------------
    // Best-of-5 completion
    // -----------------------------------------------------------------------

    @Test fun `best-of-5 3-0 sets - match over after winning 3rd set`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))
        repeat(3) {
            assertFalse(s.isMatchOver)
            s = s.winSet()
        }
        assertTrue(s.isMatchOver)
        assertEquals(3, s.match.setsWonYou)
    }

    @Test fun `best-of-5 3-1 sets - match over after 4th set`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))

        s = s.winSet()                      // you 1:0
        s = s.winSetAndContinue(Team.OPP)   // opp wins — you 1:1
        s = s.winSet()                      // you 2:1
        s = s.winSet()                      // you 3:1 — match over
        assertTrue(s.isMatchOver)
        assertEquals(3, s.match.setsWonYou)
        assertEquals(1, s.match.setsWonOpp)
    }

    @Test fun `best-of-5 3-2 sets - match over after 5th set`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))

        s = s.winSet()                      // you 1:0
        s = s.winSetAndContinue(Team.OPP)   // opp wins — 1:1
        s = s.winSet()                      // you 2:1
        s = s.winSetAndContinue(Team.OPP)   // opp wins — 2:2
        assertFalse(s.isMatchOver)
        s = s.winSet()                      // you 3:2 — match over
        assertTrue(s.isMatchOver)
        assertEquals(3, s.match.setsWonYou)
        assertEquals(2, s.match.setsWonOpp)
    }

    @Test fun `best-of-5 - not over at 2-0 sets`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))
        s = s.winSet()
        s = s.winSet()
        assertFalse(s.isMatchOver)
    }

    @Test fun `best-of-5 - not over at 2-2 sets`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))
        repeat(2) { s = s.winSet() }
        repeat(2) { s = s.winSetAndContinue(Team.OPP) }
        assertFalse(s.isMatchOver)
        assertEquals(2, s.match.setsWonYou)
        assertEquals(2, s.match.setsWonOpp)
    }

    // -----------------------------------------------------------------------
    // isMatchOver flips exactly once
    // -----------------------------------------------------------------------

    @Test fun `isMatchOver is false on the point before match ends`() {
        // One point away from winning the match: 5 games, 40-0, opp serving
        val lastPoint = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertFalse(lastPoint.isMatchOver)

        val afterWin = lastPoint.you()
        assertTrue(afterWin.isMatchOver)
    }

    // -----------------------------------------------------------------------
    // Frozen state after match ends
    // -----------------------------------------------------------------------

    @Test fun `scoring after match ends returns identical snapshot`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        s = s.winSetAndContinue(Team.YOU)
        s = s.winSet()
        assertTrue(s.isMatchOver)

        val frozen = s.you()
        assertEquals(s, frozen)

        val frozenOpp = s.opp()
        assertEquals(s, frozenOpp)
    }

    @Test fun `scoring 10 more points after match ends keeps snapshot identical`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        s = s.winSetAndContinue(Team.YOU)
        s = s.winSet()
        assertTrue(s.isMatchOver)

        val finalState = s
        repeat(10) {
            s = if (it % 2 == 0) s.you() else s.opp()
        }
        assertEquals(finalState, s)
    }

    @Test fun `stats do not increment after match ends`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        s = s.winSet()
        s = s.winSet()
        val pointsAtEnd = s.stats.totalPlayedPoints
        val gamesAtEnd = s.match.setsWonYou

        s = s.you().opp().you()
        assertEquals(pointsAtEnd, s.stats.totalPlayedPoints)
        assertEquals(gamesAtEnd, s.match.setsWonYou)
    }

    // -----------------------------------------------------------------------
    // Undo from match-complete state
    // -----------------------------------------------------------------------

    @Test fun `undo from match-complete restores isMatchOver=false`() {
        // Build state that's one point away from match end
        val lastPoint = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        val matchOver = lastPoint.you()
        assertTrue(matchOver.isMatchOver)

        // "Undo" by restoring lastPoint
        val undone = lastPoint
        assertFalse(undone.isMatchOver)
    }

    @Test fun `undo from match-complete - pill recalculates to MATCH_POINT`() {
        val lastPoint = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        val matchOver = lastPoint.you()
        assertTrue(matchOver.isMatchOver)

        // After undo, pill should be MATCH_POINT (still at match point)
        val undone = lastPoint
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(undone))
        assertFalse(undone.isMatchOver)
    }

    @Test fun `undo from set-complete restores set state`() {
        // One point away from set win (but not match win)
        val lastPoint = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        val setWon = lastPoint.you()
        assertEquals(1, setWon.match.setsWonYou)
        assertEquals(0, setWon.set.youGames)  // new set starts

        // Undo → back to 5 games, 40-0
        val undone = lastPoint
        assertEquals(0, undone.match.setsWonYou)
        assertEquals(5, undone.set.youGames)
        assertEquals(3, undone.game.youPoints)
        assertFalse(undone.isMatchOver)
    }

    // -----------------------------------------------------------------------
    // Opp wins the match
    // -----------------------------------------------------------------------

    @Test fun `opp wins best-of-3 match 2-0`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) { s = s.winSetAndContinue(Team.OPP) }
        assertTrue(s.isMatchOver)
        assertEquals(0, s.match.setsWonYou)
        assertEquals(2, s.match.setsWonOpp)
    }
}
