package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Tests [MatchEngine.computeScoreLayout] — the diagonal position of the two score numbers.
 *
 * Spec §6 / D2:
 *   even rallyIndex (points played in current game) → You bottom-right, Opp top-left
 *   odd  rallyIndex                                  → You bottom-left, Opp top-right
 *
 * rallyIndex = youPoints + oppPoints. Resets to 0 at the start of every new game.
 * Applies to: regular games, deuce/advantage states, and tie-break (all use the same formula).
 */
class DiagonalPositionTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun layoutA() = ScoreLayout(ScorePosition.BOTTOM_RIGHT, ScorePosition.TOP_LEFT)
    private fun layoutB() = ScoreLayout(ScorePosition.BOTTOM_LEFT, ScorePosition.TOP_RIGHT)

    private fun Snapshot.layout() = MatchEngine.computeScoreLayout(this)

    // -----------------------------------------------------------------------
    // Regular game: even/odd alternation
    // -----------------------------------------------------------------------

    @Test fun `game start rally 0 → layout A`() {
        assertEquals(layoutA(), makeSnapshot().layout())
    }

    @Test fun `rally 1 → layout B`() {
        val s = makeSnapshot().you()  // 1 point played
        assertEquals(layoutB(), s.layout())
    }

    @Test fun `rally 2 → layout A`() {
        val s = makeSnapshot().you().opp()
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `rally 3 → layout B`() {
        val s = makeSnapshot().you().you().opp()
        assertEquals(layoutB(), s.layout())
    }

    @Test fun `layout alternates with every point`() {
        var s = makeSnapshot()
        val expected = listOf(layoutA(), layoutB(), layoutA(), layoutB(), layoutA(), layoutB())
        for (exp in expected) {
            assertEquals(exp, s.layout())
            s = s.you()
        }
    }

    // -----------------------------------------------------------------------
    // Deuce / Advantage: same even/odd formula, no special casing
    // -----------------------------------------------------------------------

    @Test fun `at deuce rally 6 even → layout A`() {
        // 3:3 = 6 points played → even → layout A
        val s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        assertEquals(6, s.game.youPoints + s.game.oppPoints)
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `ADV_YOU rally 7 odd → layout B`() {
        val s = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU)
        assertEquals(7, s.game.youPoints + s.game.oppPoints)
        assertEquals(layoutB(), s.layout())
    }

    @Test fun `ADV_OPP rally 7 odd → layout B`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP)
        assertEquals(layoutB(), s.layout())
    }

    @Test fun `second deuce rally 8 even → layout A`() {
        // After ADV lost → DEUCE again: 4:4 = rally 8 (even) → layout A
        val s = makeSnapshot(youPoints = 4, oppPoints = 4, phase = GamePhase.DEUCE)
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `deuce → ADV_YOU → back to DEUCE changes layout correctly via engine`() {
        var s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        assertEquals(layoutA(), s.layout())  // rally 6, even

        s = s.you()  // ADV_YOU, rally 7
        assertEquals(layoutB(), s.layout())

        s = s.opp()  // back to DEUCE, rally 8
        assertEquals(layoutA(), s.layout())
    }

    // -----------------------------------------------------------------------
    // Golden and Star Point phases
    // -----------------------------------------------------------------------

    @Test fun `GOLDEN phase rally 6 → layout A`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `STAR_POINT phase rally even → layout A`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5, phase = GamePhase.STAR_POINT, starAdvCount = 2
        )
        assertEquals(10 % 2, 0)
        assertEquals(layoutA(), s.layout())
    }

    // -----------------------------------------------------------------------
    // Tie-break: same formula (TB points accumulate as raw rally index)
    // -----------------------------------------------------------------------

    @Test fun `TB start rally 0 → layout A`() {
        val s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `TB after 1 point rally 1 → layout B`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 1, oppPoints = 0
        )
        assertEquals(layoutB(), s.layout())
    }

    @Test fun `TB after 4 points rally 4 → layout A`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 3, oppPoints = 1
        )
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `TB layout alternates each scored point via engine`() {
        var s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)
        val expected = listOf(layoutA(), layoutB(), layoutA(), layoutB())
        for (exp in expected) {
            assertEquals(exp, s.layout())
            s = s.you()
        }
    }

    // -----------------------------------------------------------------------
    // Reset on new game
    // -----------------------------------------------------------------------

    @Test fun `new game always starts with layout A`() {
        // Win a game from 40-0 → new game starts
        val s = makeSnapshot().scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        // New game, rally = 0 → layout A
        assertEquals(0, s.game.youPoints + s.game.oppPoints)
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `layout resets after game won mid-rally`() {
        // Reach rally 5 (odd → layout B), then win the next game from a fresh 40-0 state
        // Easier: play to 40-30 (rally 5, odd), then win (game over), new game
        val s = makeSnapshot()
            .you().opp().you().opp().you()  // 3:2, rally 5 → layout B
        assertEquals(layoutB(), s.layout())

        // Win the game (you score at 3:2 → 4:2 → game won)
        val afterWin = s.you()
        assertEquals(1, afterWin.set.youGames)
        assertEquals(layoutA(), afterWin.layout())  // new game, rally 0 → layout A
    }

    @Test fun `layout resets after set win`() {
        var s = makeSnapshot()
        repeat(6) { s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU) }
        // New set, new game, rally = 0
        assertEquals(layoutA(), s.layout())
    }

    @Test fun `layout resets after tie-break win`() {
        val tb = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 7, oppPoints = 5   // rally 12, even → layout A before win
        )
        val afterTB = tb.you()  // 8:5 wins TB
        // New set, new game
        assertEquals(layoutA(), afterTB.layout())
    }
}
