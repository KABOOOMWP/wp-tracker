package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests the snapshot/undo system.
 * Covers spec sections 17, 18.1, and edge case I.5.
 *
 * The engine itself is pure (returns new snapshots).
 * Undo is managed by the caller: they keep a List<Snapshot> stack.
 * These tests verify the snapshots are truly immutable and that
 * the state before each scored point can always be recovered.
 */
class UndoTest {

    // -----------------------------------------------------------------------
    // Snapshot immutability
    // -----------------------------------------------------------------------

    @Test fun `scoring returns a new snapshot and original unchanged`() {
        val original = makeSnapshot()
        val after = original.you()
        assertEquals(0, original.game.youPoints) // original untouched
        assertEquals(1, after.game.youPoints)
    }

    @Test fun `undo by popping stack restores previous state`() {
        val s0 = makeSnapshot()
        val s1 = s0.you()
        val s2 = s1.opp()

        // Undo s2 → pop → back to s1
        val stack = mutableListOf(s0, s1, s2)
        stack.removeAt(stack.lastIndex)
        assertEquals(s1, stack.last())
        assertEquals(1, stack.last().game.youPoints)
        assertEquals(0, stack.last().game.oppPoints)
    }

    @Test fun `multiple undos work down to initial state`() {
        val s0 = makeSnapshot()
        val stack = mutableListOf(s0)
        repeat(5) { stack.add(stack.last().you()) }

        assertEquals(5, stack.last().stats.totalPlayedPoints)  // 5 points scored total
        repeat(5) { stack.removeAt(stack.lastIndex) }
        assertEquals(s0, stack.last())
    }

    // -----------------------------------------------------------------------
    // Undo across game boundary
    // -----------------------------------------------------------------------

    @Test fun `undo after game win restores game-in-progress`() {
        val beforeWin = makeSnapshot(youPoints = 3, oppPoints = 0)
        val afterWin = beforeWin.you() // game won

        assertEquals(1, afterWin.set.youGames)
        assertEquals(0, afterWin.game.youPoints)

        // Undo → back to before the winning point
        val undone = beforeWin // simulating stack pop
        assertEquals(3, undone.game.youPoints)
        assertEquals(0, undone.set.youGames)
    }

    // -----------------------------------------------------------------------
    // Undo across set boundary
    // -----------------------------------------------------------------------

    @Test fun `undo after set win restores last game of previous set`() {
        // Build up to last game of set 1
        var s = makeSnapshot()
        val history = mutableListOf(s)
        repeat(5) {
            repeat(4) { s = s.you(); history.add(s) }
        }
        // Now 5-0, win one more game → set done
        repeat(4) { s = s.you(); history.add(s) }
        assertEquals(1, s.match.setsWonYou)
        assertEquals(0, s.set.youGames)

        // Undo the set-winning point to land back at 5-0 / game 40-0
        repeat(1) { history.removeAt(history.lastIndex) }
        val restored = history.last()
        assertEquals(0, restored.match.setsWonYou)
        assertEquals(5, restored.set.youGames)
        assertEquals(3, restored.game.youPoints)
    }

    // -----------------------------------------------------------------------
    // Undo across tie-break boundary
    // -----------------------------------------------------------------------

    @Test fun `undo after tie-break win restores tie-break in progress`() {
        val tbState = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6, oppPoints = 5
        )
        val afterTb = tbState.you() // wins TB
        assertEquals(1, afterTb.match.setsWonYou)

        // Undo → we're back in the TB at 6:5
        val undone = tbState
        assertEquals(GameMode.TIEBREAK, undone.game.mode)
        assertEquals(6, undone.game.youPoints)
        assertEquals(5, undone.game.oppPoints)
        assertEquals(0, undone.match.setsWonYou)
    }

    // -----------------------------------------------------------------------
    // Undo across match boundary
    // -----------------------------------------------------------------------

    @Test fun `undo after match end restores last game`() {
        val beforeMatchWin = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1, setsWonOpp = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0
        )
        val matchOver = beforeMatchWin.you()
        assertTrue(matchOver.isMatchOver)

        // Undo → match not over
        val undone = beforeMatchWin
        assertFalse(undone.isMatchOver)
        assertEquals(5, undone.set.youGames)
        assertEquals(3, undone.game.youPoints)
    }

    // -----------------------------------------------------------------------
    // Undo after Star Point
    // -----------------------------------------------------------------------

    @Test fun `undo from STAR_POINT back to STAR_ADV_OPP`() {
        val beforeStarPoint = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 4,
            phase = GamePhase.STAR_ADV_OPP,
            starAdvCount = 1
        )
        val starPoint = beforeStarPoint.you() // advantage lost → STAR_POINT
        assertEquals(GamePhase.STAR_POINT, starPoint.game.phase)

        val undone = beforeStarPoint
        assertEquals(GamePhase.STAR_ADV_OPP, undone.game.phase)
        assertEquals(1, undone.game.starAdvCount)
    }

    // -----------------------------------------------------------------------
    // Stats tracking across scoring
    // -----------------------------------------------------------------------

    @Test fun `totalPlayedPoints increments each score`() {
        val s = makeSnapshot().you().opp().you()
        assertEquals(3, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints persists across game boundaries`() {
        var s = makeSnapshot()
        repeat(4) { s = s.you() } // win game (4 points)
        repeat(3) { s = s.opp() } // 3 more
        assertEquals(7, s.stats.totalPlayedPoints)
    }

    @Test fun `golden decider stats tracked`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN
        ).you() // you win the golden point
        assertEquals(1, s.stats.goldenDecidersPlayed)
        assertEquals(1, s.stats.goldenDecidersWonYou)
    }

    @Test fun `golden decider opp wins not counted for you`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN
        ).opp()
        assertEquals(1, s.stats.goldenDecidersPlayed)
        assertEquals(0, s.stats.goldenDecidersWonYou)
    }
}
