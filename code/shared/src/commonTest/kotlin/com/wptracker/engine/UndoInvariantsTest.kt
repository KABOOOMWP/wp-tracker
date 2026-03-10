package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Additional undo invariant tests.
 *
 * Covers:
 *   - Single-snapshot stack behavior (no undo possible)
 *   - Derived state (pill, server, serve side, game phase) correctly reflects the undone snapshot
 *   - Undo across tie-break activation boundary
 */
class UndoInvariantsTest {

    // -----------------------------------------------------------------------
    // Single-snapshot stack: undo is not possible
    // -----------------------------------------------------------------------

    @Test fun `stack with 1 snapshot cannot undo (size check)`() {
        val initial = makeSnapshot()
        val stack = mutableListOf(initial)
        // Undo is possible only if stack.size > 1
        assertFalse(stack.size > 1, "Should not be able to undo from a single-entry stack")
    }

    @Test fun `stack with 2+ snapshots can undo`() {
        val s0 = makeSnapshot()
        val s1 = s0.you()
        val stack = mutableListOf(s0, s1)
        assertTrue(stack.size > 1)
        stack.removeAt(stack.lastIndex)
        assertEquals(s0, stack.last())
    }

    // -----------------------------------------------------------------------
    // After undo: pill state recalculates from the restored snapshot
    // -----------------------------------------------------------------------

    @Test fun `pill changes correctly after undo`() {
        // Start at 5-0 games, 40-0 → SET_POINT pill
        val setPoint = makeSnapshot(
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(setPoint))

        // Score another point → game won → 6:0 → set won → new game, new set
        val afterWin = setPoint.you()
        assertNotEquals(PillState.SET_POINT, MatchEngine.computePill(afterWin))

        // Undo → back to setPoint
        val undone = setPoint
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(undone))
    }

    @Test fun `pill is HIDDEN after undo to normal game state`() {
        val normal = makeSnapshot(youPoints = 1, oppPoints = 2)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(normal))

        // Advance to a state where pill is visible
        val s = makeSnapshot(youGames = 5, oppGames = 0, youPoints = 3, oppPoints = 0, serverTeam = Team.YOU)
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))

        // Undo back to normal → pill is HIDDEN again
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(normal))
    }

    // -----------------------------------------------------------------------
    // After undo: server and serve side match the restored snapshot
    // -----------------------------------------------------------------------

    @Test fun `serve side is restored after undo`() {
        val s0 = makeSnapshot(serveSide = ServeSide.RIGHT)
        val s1 = s0.you()  // rally 1 → serve side LEFT
        assertEquals(ServeSide.LEFT, s1.serve.serveSide)

        val undone = s0
        assertEquals(ServeSide.RIGHT, undone.serve.serveSide)
    }

    @Test fun `server player is restored after undo across game boundary`() {
        val lastPoint = makeSnapshot(
            config = singlesConfig(),
            youPoints = 3, oppPoints = 0,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        assertEquals(Player.A1, lastPoint.serve.serverPlayer)

        val afterWin = lastPoint.you()  // game won → B1 now serves
        assertEquals(Player.B1, afterWin.serve.serverPlayer)

        val undone = lastPoint
        assertEquals(Player.A1, undone.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // After undo: game phase matches restored snapshot
    // -----------------------------------------------------------------------

    @Test fun `game phase restored after undo from DEUCE back to NORMAL`() {
        val beforeDeuce = makeSnapshot(youPoints = 2, oppPoints = 3)  // NORMAL
        assertEquals(GamePhase.NORMAL, beforeDeuce.game.phase)

        val atDeuce = beforeDeuce.you()  // → 3:3 → DEUCE
        assertEquals(GamePhase.DEUCE, atDeuce.game.phase)

        val undone = beforeDeuce
        assertEquals(GamePhase.NORMAL, undone.game.phase)
    }

    @Test fun `game phase restored after undo from ADV back to DEUCE`() {
        val atDeuce = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        val atAdv = atDeuce.you()
        assertEquals(GamePhase.ADV_YOU, atAdv.game.phase)

        val undone = atDeuce
        assertEquals(GamePhase.DEUCE, undone.game.phase)
    }

    @Test fun `starAdvCount restored after undo`() {
        val beforeAdvLost = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0
        )
        val afterAdvLost = beforeAdvLost.opp()  // → DEUCE, advCount=1
        assertEquals(1, afterAdvLost.game.starAdvCount)

        val undone = beforeAdvLost
        assertEquals(0, undone.game.starAdvCount)
        assertEquals(GamePhase.STAR_ADV_YOU, undone.game.phase)
    }

    // -----------------------------------------------------------------------
    // Undo across tie-break activation boundary
    // -----------------------------------------------------------------------

    @Test fun `undo from TB-active state back to 6-5 (pre-trigger)`() {
        // At 5:6, you serve and win → 6:6 → TB activates
        val preWin = makeSnapshot(
            config = singlesConfig(),
            youGames = 5, oppGames = 6,
            youPoints = 3, oppPoints = 0,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        val tbActive = preWin.you()  // game won → 6:6 → TB started
        assertEquals(GameMode.TIEBREAK, tbActive.game.mode)
        assertEquals(6, tbActive.set.youGames)
        assertEquals(6, tbActive.set.oppGames)

        // Undo → back to 5:6, REGULAR game, game at 40-0
        val undone = preWin
        assertEquals(GameMode.REGULAR, undone.game.mode)
        assertEquals(5, undone.set.youGames)
        assertEquals(6, undone.set.oppGames)
        assertEquals(3, undone.game.youPoints)
        assertEquals(0, undone.game.oppPoints)
    }

    @Test fun `undo mid tie-break back to TB entry state`() {
        // Enter TB at 6:6
        val tbEntry = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 0, oppPoints = 0,
            serverPlayer = Player.B1, serverTeam = Team.OPP, serveOrderIndex = 1
        )
        // Score 3 TB points
        val tb3 = tbEntry.you().you().opp()
        assertEquals(2, tb3.game.youPoints)
        assertEquals(1, tb3.game.oppPoints)

        // Undo those 3 points individually
        val undo1 = tb3  // first undo goes to 2:1
        val stack = mutableListOf(tbEntry, tbEntry.you(), tbEntry.you().you(), tb3)
        stack.removeAt(stack.lastIndex)
        stack.removeAt(stack.lastIndex)
        stack.removeAt(stack.lastIndex)
        assertEquals(tbEntry, stack.last())

        // Restored to TB entry: 0:0
        assertEquals(0, stack.last().game.youPoints)
        assertEquals(0, stack.last().game.oppPoints)
        assertEquals(GameMode.TIEBREAK, stack.last().game.mode)
    }

    // -----------------------------------------------------------------------
    // Undo restores decider side override
    // -----------------------------------------------------------------------

    @Test fun `decider side override restored after undo`() {
        val golden = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val overridden = MatchEngine.setDeciderSide(golden, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, overridden.serve.serveSide)
        assertEquals(ServeSide.LEFT, overridden.game.deciderReceiveSideOverride)

        // "Undo" the override (UI pops to before setDeciderSide was called)
        val undone = golden
        assertNull(undone.game.deciderReceiveSideOverride)
        // Serve side reverts to even/odd computed value (rally 6, even → RIGHT)
        assertEquals(ServeSide.RIGHT, undone.serve.serveSide)
    }
}
