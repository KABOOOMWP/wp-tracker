package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests serve rotation for 1v1 and 2v2, serve side logic, and white-dot position.
 * Covers spec sections 8, 12, 13, and D.
 */
class ServeRotationTest {

    // Helper: score enough points to win a game (4 uncontested)
    private fun Snapshot.winGameYou() = scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
    private fun Snapshot.winGameOpp() = scoreMany(Team.OPP, Team.OPP, Team.OPP, Team.OPP)

    // -----------------------------------------------------------------------
    // Serve side within a game (even=RIGHT, odd=LEFT)
    // -----------------------------------------------------------------------

    @Test fun `game start rally 0 → RIGHT`() {
        val s = makeSnapshot(serverTeam = Team.YOU)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `after 1 point rally 1 → LEFT`() {
        val s = makeSnapshot(serverTeam = Team.YOU).you()
        assertEquals(ServeSide.LEFT, s.serve.serveSide)
    }

    @Test fun `after 2 points rally 2 → RIGHT`() {
        val s = makeSnapshot(serverTeam = Team.YOU).you().opp()
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `serve side alternates with every point in regular game`() {
        var s = makeSnapshot()
        val expected = listOf(RIGHT, LEFT, RIGHT, LEFT, RIGHT, LEFT)
        for (side in expected) {
            assertEquals(side, s.serve.serveSide)
            s = s.you()
        }
    }

    // -----------------------------------------------------------------------
    // 1v1 serve rotation (game-level)
    // -----------------------------------------------------------------------

    // Initial: A1 serves game 1
    // After game 1 won: B1 serves game 2
    // After game 2 won: A1 serves game 3

    @Test fun `1v1 game 1 server is A1`() {
        val s = makeSnapshot(config = singlesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `1v1 after game 1 → B1 serves`() {
        val s = makeSnapshot(config = singlesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou()
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `1v1 after game 2 → A1 serves again`() {
        val s = makeSnapshot(config = singlesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou()
            .winGameOpp()
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `1v1 serve resets to RIGHT at game start`() {
        val s = makeSnapshot(config = singlesConfig(), serveOrderIndex = 0)
            .winGameYou() // B1 now serves
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    // -----------------------------------------------------------------------
    // 2v2 serve rotation (game-level)
    // -----------------------------------------------------------------------

    // Order: A1 → B1 → A2 → B2 → A1 → …

    @Test fun `2v2 game 1 A1 serves`() {
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    @Test fun `2v2 game 2 B1 serves`() {
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou()
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 game 3 A2 serves`() {
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou().winGameOpp()
        assertEquals(Player.A2, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `2v2 game 4 B2 serves`() {
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou().winGameOpp().winGameYou()
        assertEquals(Player.B2, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 game 5 wraps back to A1`() {
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou().winGameOpp().winGameYou().winGameOpp()
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    @Test fun `2v2 serve order persists across set boundary`() {
        // After set 1 (won at 6:0 with A1 serving all), the rotation continues
        // 6 games → rotations: A1,B1,A2,B2,A1,B1 → next is A2 (index 2)
        var s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
        repeat(6) { s = s.winGameYou() }
        // setsWonYou == 1, new set started
        assertEquals(1, s.match.setsWonYou)
        // After 6 games, serveOrderIndex should be at index 2 (A2)
        assertEquals(Player.A2, s.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // Server team tracking
    // -----------------------------------------------------------------------

    @Test fun `A1 and A2 are always team YOU`() {
        val s1 = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou().winGameOpp() // now A2
        assertEquals(Team.YOU, s1.serve.serverTeam)
    }

    @Test fun `B1 and B2 are always team OPP`() {
        val s1 = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou() // B1
        assertEquals(Team.OPP, s1.serve.serverTeam)

        val s2 = s1.winGameOpp().winGameYou() // B2
        assertEquals(Team.OPP, s2.serve.serverTeam)
    }

    // -----------------------------------------------------------------------
    // OPP-first serve orders (custom order from new setup UI)
    // -----------------------------------------------------------------------

    // 1v1 with OPP serving first: order = [B1, A1]

    @Test fun `1v1 opp-first — game 1 server is B1`() {
        val s = makeSnapshot(config = singlesConfigOppFirst(), serverPlayer = Player.B1,
            serverTeam = Team.OPP, serveOrderIndex = 0)
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `1v1 opp-first — game 2 server is A1`() {
        val s = makeSnapshot(config = singlesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp()
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `1v1 opp-first — game 3 wraps back to B1`() {
        val s = makeSnapshot(config = singlesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp()
            .winGameYou()
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    // 2v2 with OPP serving first: order = [B1, A1, B2, A2]

    @Test fun `2v2 opp-first — game 1 B1 serves`() {
        val s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1,
            serverTeam = Team.OPP, serveOrderIndex = 0)
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 opp-first — game 2 A1 serves`() {
        val s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp()
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `2v2 opp-first — game 3 B2 serves`() {
        val s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp()
            .winGameYou()
        assertEquals(Player.B2, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 opp-first — game 4 A2 serves`() {
        val s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp()
            .winGameYou()
            .winGameOpp()
        assertEquals(Player.A2, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `2v2 opp-first — game 5 wraps back to B1`() {
        val s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
            .winGameOpp().winGameYou().winGameOpp().winGameYou()
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 opp-first — serve order persists across set boundary`() {
        // 6 games: B1(0),A1(1),B2(2),A2(3),B1(0),A1(1) → last served=A1, next=B2 (idx 2)
        var s = makeSnapshot(config = doublesConfigOppFirst(), serverPlayer = Player.B1, serveOrderIndex = 0)
        repeat(6) { s = s.winGameOpp() }
        assertEquals(1, s.match.setsWonOpp)
        assertEquals(Player.B2, s.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // awaitingServePick does not block engine scoring
    // -----------------------------------------------------------------------

    @Test fun `engine score works even when awaitingServePick is true`() {
        // After game 1 ends in doubles, awaitingServePick = true.
        // The engine still returns a new snapshot (UI is responsible for blocking the tap zone).
        val s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
            .winGameYou() // awaitingServePick = true now
        assertTrue(s.awaitingServePick)
        val next = MatchEngine.score(s, Team.YOU)
        assertNotNull(next)
        // mid-game point: awaitingServePick preserved (UI still shows picker)
        assertTrue(next.awaitingServePick)
    }

    // -----------------------------------------------------------------------
    // White dot position (server team × serve side)
    // -----------------------------------------------------------------------

    @Test fun `OPP serves RIGHT → white dot top-right`() {
        val s = makeSnapshot(serverTeam = Team.OPP, serveSide = ServeSide.RIGHT)
        assertEquals(Team.OPP, s.serve.serverTeam)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `OPP serves LEFT → white dot top-left`() {
        val s = makeSnapshot(serverTeam = Team.OPP, serveSide = ServeSide.LEFT)
        assertEquals(Team.OPP, s.serve.serverTeam)
        assertEquals(ServeSide.LEFT, s.serve.serveSide)
    }

    @Test fun `YOU serve RIGHT → white dot bottom-right`() {
        val s = makeSnapshot(serverTeam = Team.YOU, serveSide = ServeSide.RIGHT)
        assertEquals(Team.YOU, s.serve.serverTeam)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `YOU serve LEFT → white dot bottom-left`() {
        val s = makeSnapshot(serverTeam = Team.YOU, serveSide = ServeSide.LEFT)
        assertEquals(Team.YOU, s.serve.serverTeam)
        assertEquals(ServeSide.LEFT, s.serve.serveSide)
    }
}

private val RIGHT = ServeSide.RIGHT
private val LEFT = ServeSide.LEFT
