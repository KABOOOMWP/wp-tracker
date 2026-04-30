package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests tie-break scoring, win conditions, and serve rotation.
 * Covers spec sections 14 and G.
 */
class TieBreakTest {

    private fun tbSnapshot(
        config: Config = singlesConfig(),
        youPoints: Int = 0,
        oppPoints: Int = 0,
        serveOrderIndex: Int = 0
    ) = makeSnapshot(
        config = config,
        youGames = 6,
        oppGames = 6,
        gameMode = GameMode.TIEBREAK,
        youPoints = youPoints,
        oppPoints = oppPoints,
        serverPlayer = config.serveOrder[serveOrderIndex],
        serverTeam = config.serveOrder[serveOrderIndex].team(),
        serveOrderIndex = serveOrderIndex
    )

    // -----------------------------------------------------------------------
    // Tie-break activation
    // -----------------------------------------------------------------------

    @Test fun `6-6 in set activates tie-break`() {
        val s = makeSnapshot(youGames = 5, oppGames = 6)
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(0, s.game.youPoints)
        assertEquals(0, s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    // -----------------------------------------------------------------------
    // Scoring in tie-break
    // -----------------------------------------------------------------------

    @Test fun `tie-break points count raw 0 1 2 3`() {
        val s = tbSnapshot().you().you().opp()
        assertEquals(2, s.game.youPoints)
        assertEquals(1, s.game.oppPoints)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
    }

    @Test fun `tie-break no deuce or advantage states`() {
        var s = tbSnapshot()
        repeat(6) { s = s.you() }
        repeat(6) { s = s.opp() } // 6:6 in TB
        assertEquals(GamePhase.NORMAL, s.game.phase) // never enters DEUCE
        assertEquals(GameMode.TIEBREAK, s.game.mode)
    }

    // -----------------------------------------------------------------------
    // Win conditions
    // -----------------------------------------------------------------------

    @Test fun `first to 7 with 2-lead wins tie-break`() {
        val s = tbSnapshot(youPoints = 6, oppPoints = 5).you() // 7:5
        assertEquals(1, s.match.setsWonYou)
        assertEquals(7, s.match.setScores.last { it.isCompleted }.youGames)
        assertEquals(6, s.match.setScores.last { it.isCompleted }.oppGames)
    }

    @Test fun `7-6 is NOT a tie-break win`() {
        val s = tbSnapshot(youPoints = 6, oppPoints = 6).you() // 7:6 → not enough
        assertEquals(0, s.match.setsWonYou)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
    }

    @Test fun `8-6 wins tie-break`() {
        val s = tbSnapshot(youPoints = 7, oppPoints = 6).you() // 8:6
        assertEquals(1, s.match.setsWonYou)
    }

    @Test fun `extended tie-break resolves at 11-9`() {
        var s = tbSnapshot()
        repeat(9) { s = s.you() }  // 9:0
        repeat(9) { s = s.opp() }  // 9:9
        s = s.you().you()           // 11:9
        assertEquals(1, s.match.setsWonYou)
    }

    // -----------------------------------------------------------------------
    // Set score recorded correctly after TB
    // -----------------------------------------------------------------------

    @Test fun `set score after TB win is 7-6`() {
        val s = tbSnapshot(youPoints = 6, oppPoints = 5).you()
        val completedSet = s.match.setScores.firstOrNull { it.isCompleted }
        assertNotNull(completedSet)
        assertEquals(7, completedSet.youGames)
        assertEquals(6, completedSet.oppGames)
    }

    // -----------------------------------------------------------------------
    // Serve side in tie-break (alternates every point)
    // -----------------------------------------------------------------------

    @Test fun `TB point 0 served from RIGHT`() {
        val s = tbSnapshot()
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `TB point 1 served from LEFT`() {
        val s = tbSnapshot().you()
        assertEquals(ServeSide.LEFT, s.serve.serveSide)
    }

    @Test fun `TB point 2 served from RIGHT`() {
        val s = tbSnapshot().you().you()
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `TB serve side alternates every point`() {
        var s = tbSnapshot()
        val expected = listOf(ServeSide.RIGHT, ServeSide.LEFT, ServeSide.RIGHT, ServeSide.LEFT, ServeSide.RIGHT)
        for (side in expected) {
            assertEquals(side, s.serve.serveSide)
            s = s.you()
        }
    }

    // -----------------------------------------------------------------------
    // Serve rotation in 1v1 tie-break
    // -----------------------------------------------------------------------

    // Serve order: A1, B1. A1 starts TB.
    // Point 0: A1 (RIGHT)
    // Point 1-2: B1
    // Point 3-4: A1
    // Point 5-6: B1

    @Test fun `1v1 TB point 0 server is start player`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0)
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `1v1 TB point 1 server switches to B1`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0).you()
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `1v1 TB point 2 still B1`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0).you().opp()
        assertEquals(Player.B1, s.serve.serverPlayer)
    }

    @Test fun `1v1 TB point 3 switches back to A1`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0).you().opp().opp()
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    @Test fun `1v1 TB point 4 still A1`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0).you().opp().opp().you()
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    @Test fun `1v1 TB point 5-6 back to B1`() {
        val s = tbSnapshot(config = singlesConfig(), serveOrderIndex = 0)
            .scoreMany(Team.YOU, Team.OPP, Team.OPP, Team.YOU, Team.YOU)
        assertEquals(Player.B1, s.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // Serve rotation in 2v2 tie-break
    // -----------------------------------------------------------------------

    // Order: A1, B1, A2, B2. TB starts at A2 (index 2).
    // Point 0: A2
    // Point 1-2: B2
    // Point 3-4: A1
    // Point 5-6: B1
    // Point 7-8: A2 again

    @Test fun `2v2 TB point 0 starts at correct player in rotation`() {
        val s = tbSnapshot(config = doublesConfig(), serveOrderIndex = 2) // A2 starts
        assertEquals(Player.A2, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `2v2 TB point 1-2 served by B2`() {
        val s = tbSnapshot(config = doublesConfig(), serveOrderIndex = 2).you()
        assertEquals(Player.B2, s.serve.serverPlayer)
        val s2 = s.opp()
        assertEquals(Player.B2, s2.serve.serverPlayer)
    }

    @Test fun `2v2 TB point 3-4 served by A1`() {
        val s = tbSnapshot(config = doublesConfig(), serveOrderIndex = 2)
            .scoreMany(Team.YOU, Team.OPP, Team.OPP)
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    @Test fun `2v2 TB point 5-6 served by B1`() {
        val s = tbSnapshot(config = doublesConfig(), serveOrderIndex = 2)
            .scoreMany(Team.YOU, Team.OPP, Team.OPP, Team.YOU, Team.YOU)
        assertEquals(Player.B1, s.serve.serverPlayer)
    }

    @Test fun `2v2 TB point 7-8 cycles back to A2`() {
        val s = tbSnapshot(config = doublesConfig(), serveOrderIndex = 2)
            .scoreMany(Team.YOU, Team.OPP, Team.OPP, Team.YOU, Team.YOU, Team.OPP, Team.OPP)
        assertEquals(Player.A2, s.serve.serverPlayer)
    }
}
