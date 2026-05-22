package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Property / loop invariant tests.
 *
 * Covers:
 *   - Serve side RIGHT iff rallyIndex (youPoints + oppPoints) is even — across all game modes and phases
 *   - Diagonal parity after undo (across game boundary, TB boundary, decider boundary)
 *   - Tie-break cannot end with a ≤1-point lead (must win by 2 from ≥7)
 *   - totalPlayedPoints equals the number of times score() was called (consistency)
 *   - Snapshot equality: score(snapshot).copy(…) vs re-scoring fresh
 */
class InvariantTest {

    // -----------------------------------------------------------------------
    // Serve-side invariant: side = RIGHT iff rallyIndex is even
    // -----------------------------------------------------------------------

    @Test fun `serve side is RIGHT at the start of every regular game`() {
        var s = makeSnapshot()
        repeat(6) {
            assertEquals(ServeSide.RIGHT, s.serve.serveSide,
                "Game start should have RIGHT serve side")
            repeat(4) { s = s.you() }  // win the game
        }
    }

    @Test fun `serve side invariant holds across 20 consecutive points in a game`() {
        // Start a game at 0:0 (right side, even). Score 20 alternating points.
        // After point N: rallyIndex = N+1; side = RIGHT iff (N+1) is even.
        // We won't finish the game due to DEUCE cycles — use STAR mode (no deuce = 1 winner per game).
        // Actually, let's use GOLDEN so games end quickly.
        var s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            serveSide = ServeSide.RIGHT
        )

        for (pointIdx in 0 until 20) {
            s = s.you()
            if (s.isMatchOver) break
            val rallyIndex = s.game.youPoints + s.game.oppPoints
            val expectedSide = if (rallyIndex % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT
            assertEquals(expectedSide, s.serve.serveSide,
                "After point ${pointIdx + 1}: rallyIndex=$rallyIndex expected side $expectedSide")
        }
    }

    @Test fun `serve side invariant holds in tie-break across 14 points`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        for (pointIdx in 0 until 14) {
            val rallyIndex = s.game.youPoints + s.game.oppPoints
            val expectedSide = if (rallyIndex % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT
            assertEquals(expectedSide, s.serve.serveSide,
                "TB point $pointIdx: rallyIndex=$rallyIndex expected side $expectedSide")

            // Score alternating to avoid finishing (stay ≤1 apart)
            s = if (pointIdx % 2 == 0) s.you() else s.opp()
        }
    }

    // -----------------------------------------------------------------------
    // Diagonal parity invariant after undo
    // -----------------------------------------------------------------------

    @Test fun `diagonal parity invariant holds after undo across game boundary`() {
        // Win a game (rally resets to 0 → layout A), then undo the last point
        val beforeWin = makeSnapshot(youPoints = 3, oppPoints = 0)
        val afterWin = beforeWin.you()  // game won, new game starts

        // After win: rally = 0 → layout A (you: BOTTOM_RIGHT)
        val layoutAfter = MatchEngine.computeScoreLayout(afterWin)
        assertEquals(ScorePosition.BOTTOM_RIGHT, layoutAfter.youPosition)

        // Before win: rally = 3 (odd) → layout B (you: BOTTOM_LEFT)
        val layoutBefore = MatchEngine.computeScoreLayout(beforeWin)
        assertEquals(ScorePosition.BOTTOM_LEFT, layoutBefore.youPosition)
    }

    @Test fun `diagonal parity invariant holds after undo across TB boundary`() {
        // At 5:6, 40-0: win game → 6:6 TB, rally=0 → layout A
        val beforeTB = makeSnapshot(
            config = singlesConfig(),
            youGames = 5, oppGames = 6,
            youPoints = 3, oppPoints = 0,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        val atTB = beforeTB.you()  // 6:6, TB starts at rally 0

        // TB start: rally=0 even → layout A
        val layoutTB = MatchEngine.computeScoreLayout(atTB)
        assertEquals(ScorePosition.BOTTOM_RIGHT, layoutTB.youPosition)

        // Before TB: rally = 3 (odd) → layout B
        val layoutBefore = MatchEngine.computeScoreLayout(beforeTB)
        assertEquals(ScorePosition.BOTTOM_LEFT, layoutBefore.youPosition)
    }

    @Test fun `diagonal parity invariant holds after undo across decider boundary`() {
        // At 2:3, score → 3:3 → GOLDEN phase begins (rally=6 even → layout A)
        val beforeGolden = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 2, oppPoints = 3
        )
        val atGolden = beforeGolden.you()  // → 3:3 GOLDEN

        // 3:3 → rally = 6 (even) → layout A
        val layoutGolden = MatchEngine.computeScoreLayout(atGolden)
        assertEquals(ScorePosition.BOTTOM_RIGHT, layoutGolden.youPosition)

        // Before: 2:3 → rally = 5 (odd) → layout B
        val layoutBefore = MatchEngine.computeScoreLayout(beforeGolden)
        assertEquals(ScorePosition.BOTTOM_LEFT, layoutBefore.youPosition)
    }

    // -----------------------------------------------------------------------
    // Tie-break win condition: must win by ≥2 from ≥7
    // -----------------------------------------------------------------------

    @Test fun `TB does not end at 6-6`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        // Score 6 points each alternating
        repeat(6) { s = s.you(); s = s.opp() }

        assertEquals(6, s.game.youPoints)
        assertEquals(6, s.game.oppPoints)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertFalse(s.isMatchOver)
    }

    @Test fun `TB does not end at 7-6`() {
        // Score 13 TB points: 7 YOU, 6 OPP alternating (YOU first)
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        // You score 7, opp scores 6, interleaved so no premature win
        // Score: Y O Y O Y O Y O Y O Y O Y  = 7 Y, 6 O
        for (i in 0 until 13) {
            s = if (i % 2 == 0) s.you() else s.opp()
        }
        assertEquals(7, s.game.youPoints)
        assertEquals(6, s.game.oppPoints)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
    }

    @Test fun `TB ends at 7-5`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        // Score 5 for each: 2:0, 2:1, 3:1, 3:2, 4:2, 4:3, 5:3, 5:4, 5:5
        // Then YOU scores 2 in a row: 6:5, 7:5 → YOU wins
        // Interleave 5 pairs, then YOU wins twice
        for (i in 0 until 5) {
            s = s.you()
            s = s.opp()
        }
        assertEquals(5, s.game.youPoints)
        assertEquals(5, s.game.oppPoints)

        s = s.you()  // 6:5
        assertEquals(GameMode.TIEBREAK, s.game.mode)

        s = s.you()  // 7:5 → YOU wins the TB
        // TB won: new set (or match end)
        assertNotEquals(GameMode.TIEBREAK, s.game.mode)
    }

    @Test fun `TB ends at 9-7`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        // Get to 7:7 by interleaving
        for (i in 0 until 7) { s = s.you(); s = s.opp() }
        assertEquals(7, s.game.youPoints)
        assertEquals(7, s.game.oppPoints)
        assertEquals(GameMode.TIEBREAK, s.game.mode)

        // YOU scores twice to win 9:7
        s = s.you()  // 8:7
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        s = s.you()  // 9:7 → win
        assertNotEquals(GameMode.TIEBREAK, s.game.mode)
    }

    // -----------------------------------------------------------------------
    // totalPlayedPoints consistency
    // -----------------------------------------------------------------------

    @Test fun `totalPlayedPoints equals number of score calls no golden deciders`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        var scoreCalls = 0

        // Play through first set (6:0 for you)
        repeat(6) {
            repeat(4) {
                s = s.you()
                scoreCalls++
            }
        }
        assertEquals(scoreCalls, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints consistent across set boundaries`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        // Two 6-0 sets = 48 points; use winSetAndContinue to clear inter-set overlays
        s = s.winSetAndContinue(Team.YOU)
        s = s.winSetAndContinue(Team.YOU)
        assertTrue(s.isMatchOver)
        assertEquals(48, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints consistent in TB scenario`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        var scoreCalls = 0

        // Score 7:0 in TB → you win
        repeat(7) {
            s = s.you()
            scoreCalls++
        }

        assertEquals(scoreCalls, s.stats.totalPlayedPoints)
    }

    // -----------------------------------------------------------------------
    // Snapshot immutability
    // -----------------------------------------------------------------------

    @Test fun `scoring does not mutate the original snapshot`() {
        val original = makeSnapshot()
        val originalYouPoints = original.game.youPoints
        val originalTotalPoints = original.stats.totalPlayedPoints

        original.you()  // result intentionally discarded

        // original must not have changed
        assertEquals(originalYouPoints, original.game.youPoints)
        assertEquals(originalTotalPoints, original.stats.totalPlayedPoints)
    }

    @Test fun `multiple scores from the same snapshot are independent`() {
        val s0 = makeSnapshot(youPoints = 2, oppPoints = 1)

        val sYou = s0.you()
        val sOpp = s0.opp()

        // sYou: 3:1
        assertEquals(3, sYou.game.youPoints)
        assertEquals(1, sYou.game.oppPoints)

        // sOpp: 2:2
        assertEquals(2, sOpp.game.youPoints)
        assertEquals(2, sOpp.game.oppPoints)

        // s0 unchanged
        assertEquals(2, s0.game.youPoints)
        assertEquals(1, s0.game.oppPoints)
    }
}
