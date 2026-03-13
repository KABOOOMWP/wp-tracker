package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Stress tests for serve rotation correctness.
 *
 * Covers:
 *   - Full 12-game 2v2 rotation (alternating wins) → TB started by A1
 *   - Extended tie-break to 17:17 → correct server + side at each point
 *   - Undo 3 TB points from 17:17 → server + side match formula
 *   - 1v1 full-set rotation continuity (12 games, then TB)
 */
class ServeRotationStressTest {

    private fun Snapshot.winGame(team: Team) = scoreMany(team, team, team, team)

    // -----------------------------------------------------------------------
    // Full 12-game 2v2 rotation → 6:6 → TB starts with A1
    // -----------------------------------------------------------------------
    //
    // Rotation: A1(0), B1(1), A2(2), B2(3), A1(0), ...
    // Games   :  0     1     2     3     4  ...
    // YOU wins games 0,2,4,6,8,10  (A1,A2,A1,A2,A1,A2 serve)
    // OPP wins games 1,3,5,7,9,11  (B1,B2,B1,B2,B1,B2 serve)
    // After 12 games (6:6): last server = B2 (idx=3)
    // TB starts with (3+1)%4 = 0 = A1

    @Test fun `2v2 full 12-game rotation - TB opener is A1`() {
        var s = makeSnapshot(
            config = doublesConfig(),
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        val order = listOf(Player.A1, Player.B1, Player.A2, Player.B2)
        val teamFor = listOf(Team.YOU, Team.OPP, Team.YOU, Team.OPP)

        // Play 12 games alternating wins (YOU, OPP, YOU, OPP, ...)
        for (gameIdx in 0 until 12) {
            val winner = if (gameIdx % 2 == 0) Team.YOU else Team.OPP
            // Verify current server before scoring
            val expectedServer = order[gameIdx % 4]
            assertEquals(expectedServer, s.serve.serverPlayer,
                "Game $gameIdx: expected server ${expectedServer}, got ${s.serve.serverPlayer}")
            s = s.winGame(winner)
        }

        // Should now be at 6:6, TB active
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(6, s.set.youGames)
        assertEquals(6, s.set.oppGames)

        // TB opener: (3+1)%4 = A1, Team.YOU
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)  // TB always starts RIGHT
    }

    @Test fun `2v2 full 12-game rotation - serveOrderIndex advances correctly each game`() {
        var s = makeSnapshot(
            config = doublesConfig(),
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        for (gameIdx in 0 until 12) {
            val expectedIdx = gameIdx % 4
            assertEquals(expectedIdx, s.serve.serveOrderIndex,
                "Game $gameIdx: expected serveOrderIndex $expectedIdx")
            val winner = if (gameIdx % 2 == 0) Team.YOU else Team.OPP
            s = s.winGame(winner)
        }
    }

    // -----------------------------------------------------------------------
    // Extended tie-break: play to 17:17
    // -----------------------------------------------------------------------
    //
    // TB start: A1 (serveOrderIndex=0), Side=RIGHT
    // Formula: point 0 → idx=0 (A1); point N>0 → idx=(0+1+(N-1)/2)%4
    // Side: even pointIndex → RIGHT, odd → LEFT

    @Test fun `2v2 extended TB - server at each point follows formula`() {
        val order = listOf(Player.A1, Player.B1, Player.A2, Player.B2)
        val config = doublesConfig()
        val startIdx = 0

        // Verify formula output for points 0..33 (34 total points reaching 17:17
        // alternating YOU/OPP so no winner yet)
        var s = makeSnapshot(
            config = config,
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = startIdx
        )

        for (pointIdx in 0 until 34) {
            val expectedOrderIdx = MatchEngine.tieBreakServerOrderIndex(pointIdx, startIdx, 4)
            val expectedPlayer = order[expectedOrderIdx]
            val expectedSide = if (pointIdx % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT

            assertEquals(expectedPlayer, s.serve.serverPlayer,
                "TB point $pointIdx: expected server $expectedPlayer")
            assertEquals(expectedSide, s.serve.serveSide,
                "TB point $pointIdx: expected side $expectedSide")

            // Score alternating to avoid a winner (stay ≤1 apart)
            s = if (pointIdx % 2 == 0) s.you() else s.opp()
        }

        // After 34 points: score is 17:17
        assertEquals(17, s.game.youPoints)
        assertEquals(17, s.game.oppPoints)
    }

    @Test fun `2v2 TB at 17-17 - undo 3 points → server and side match formula`() {
        val config = doublesConfig()
        val startIdx = 0
        val order = listOf(Player.A1, Player.B1, Player.A2, Player.B2)

        // Build stack of 35 snapshots (points 0..34)
        var s = makeSnapshot(
            config = config,
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = startIdx
        )
        val stack = mutableListOf(s)
        for (i in 0 until 34) {
            s = if (i % 2 == 0) s.you() else s.opp()
            stack.add(s)
        }
        // stack.last() is at point-index 34 (17:17)

        // Undo 3 points → back to point-index 31 (16:15 or similar)
        stack.removeAt(stack.lastIndex)
        stack.removeAt(stack.lastIndex)
        stack.removeAt(stack.lastIndex)
        val undone = stack.last()

        val pointIdx = 31  // 34 - 3
        val expectedOrderIdx = MatchEngine.tieBreakServerOrderIndex(pointIdx, startIdx, 4)
        val expectedPlayer = order[expectedOrderIdx]
        val expectedSide = if (pointIdx % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT

        assertEquals(expectedPlayer, undone.serve.serverPlayer)
        assertEquals(expectedSide, undone.serve.serveSide)
    }

    // -----------------------------------------------------------------------
    // 1v1 full-set rotation continuity (12 games, then TB)
    // -----------------------------------------------------------------------
    //
    // 1v1 rotation: A1(0), B1(1), A1(0), ...
    // After 12 games (6:6): last server = B1 (game 11, idx=1)
    // TB starts with (1+1)%2 = 0 = A1

    @Test fun `1v1 full 12-game rotation - TB opener is A1`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        for (gameIdx in 0 until 12) {
            val winner = if (gameIdx % 2 == 0) Team.YOU else Team.OPP
            s = s.winGame(winner)
        }

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `1v1 full 12-game rotation - alternating server each game`() {
        val order = listOf(Player.A1, Player.B1)
        var s = makeSnapshot(
            config = singlesConfig(),
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )

        for (gameIdx in 0 until 12) {
            val expectedPlayer = order[gameIdx % 2]
            assertEquals(expectedPlayer, s.serve.serverPlayer,
                "1v1 game $gameIdx: expected server $expectedPlayer")
            val winner = if (gameIdx % 2 == 0) Team.YOU else Team.OPP
            s = s.winGame(winner)
        }
    }

    // -----------------------------------------------------------------------
    // Serve side alternates correctly across long sequences
    // -----------------------------------------------------------------------

    @Test fun `serve side alternates LEFT-RIGHT for every point in regular game`() {
        var s = makeSnapshot(serveSide = ServeSide.RIGHT)  // rally 0, even → RIGHT

        // Score 8 points, verifying alternating sides
        val expectedSides = listOf(
            ServeSide.LEFT,   // after point 1 (rally 1, odd)
            ServeSide.RIGHT,  // after point 2 (rally 2, even)
            ServeSide.LEFT,   // after point 3
            ServeSide.RIGHT,  // after point 4 → game over (0:4 win for you), rally resets
            // New game starts at rally 0 → RIGHT
            ServeSide.LEFT,   // after point 1 of new game
            ServeSide.RIGHT,  // after point 2
            ServeSide.LEFT,   // after point 3
        )

        for ((idx, expected) in expectedSides.withIndex()) {
            s = s.you()
            if (!s.isMatchOver) {
                assertEquals(expected, s.serve.serveSide,
                    "After scoring point ${idx + 1}: expected $expected")
            }
        }
    }

    @Test fun `serve side resets to RIGHT at start of each new regular game`() {
        var s = makeSnapshot()
        // Win 3 games
        repeat(3) {
            repeat(4) { s = s.you() }
            // At the start of each new game, rally index = 0 → RIGHT
            assertEquals(ServeSide.RIGHT, s.serve.serveSide,
                "New game should start with RIGHT serve side")
        }
    }

    @Test fun `serve side resets to RIGHT when TB starts`() {
        var s = makeSnapshot(
            config = singlesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        )
        // Win game to trigger TB (6:6)
        s = s.winGame(Team.YOU)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }
}
