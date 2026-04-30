package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests for the deferred-serve-pick flow in doubles (spec: game 2 server pick).
 */
class ServePickTest {

    private fun Snapshot.winGameYou() = scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
    private fun Snapshot.winGameOpp() = scoreMany(Team.OPP, Team.OPP, Team.OPP, Team.OPP)

    // -----------------------------------------------------------------------
    // awaitingServePick flag
    // -----------------------------------------------------------------------

    @Test fun `awaitingServePick is true after game 1 ends in doubles`() {
        val s = MatchEngine.initialSnapshot(doublesConfigDeferred(), 0L).winGameYou()
        assertTrue(s.awaitingServePick)
    }

    @Test fun `awaitingServePick is false for singles`() {
        val s = MatchEngine.initialSnapshot(singlesConfig(), 0L).winGameYou()
        assertFalse(s.awaitingServePick)
    }

    @Test fun `awaitingServePick is false at start of match`() {
        val s = MatchEngine.initialSnapshot(doublesConfigDeferred(), 0L)
        assertFalse(s.awaitingServePick)
    }

    // -----------------------------------------------------------------------
    // pickOpponentFirstServer — order construction
    // -----------------------------------------------------------------------

    @Test fun `pickOpponentFirstServer A1 first pick B2 gives order A1 B2 A2 B1`() {
        // YOU serves first (A1), OPP deferred → pick B2
        val config = Config(
            bestOf = 3, ruleMode = RuleMode.STANDARD, playMode = PlayMode.DOUBLES,
            serveOrder = listOf(Player.A1, Player.B1, Player.A2, Player.B2) // B1 is placeholder
        )
        val afterGame1 = MatchEngine.initialSnapshot(config, 0L).winGameYou()
        val picked = MatchEngine.pickOpponentFirstServer(afterGame1, Player.B2)

        assertEquals(listOf(Player.A1, Player.B2, Player.A2, Player.B1), picked.config.serveOrder)
        assertEquals(Player.B2, picked.serve.serverPlayer)
        assertEquals(Team.OPP, picked.serve.serverTeam)
        assertFalse(picked.awaitingServePick)
        assertTrue(picked.serve.opponentServerConfirmed)
    }

    @Test fun `pickOpponentFirstServer A2 first pick B1 gives order A2 B1 A1 B2`() {
        val config = Config(
            bestOf = 3, ruleMode = RuleMode.STANDARD, playMode = PlayMode.DOUBLES,
            serveOrder = listOf(Player.A2, Player.B1, Player.A1, Player.B2) // B1 is placeholder
        )
        val afterGame1 = MatchEngine.initialSnapshot(config, 0L).winGameYou()
        val picked = MatchEngine.pickOpponentFirstServer(afterGame1, Player.B1)

        assertEquals(listOf(Player.A2, Player.B1, Player.A1, Player.B2), picked.config.serveOrder)
        assertEquals(Player.B1, picked.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // awaitingServePick does NOT re-trigger after confirmed
    // -----------------------------------------------------------------------

    @Test fun `awaitingServePick does not re-trigger after opponentServerConfirmed`() {
        val afterGame1 = MatchEngine.initialSnapshot(doublesConfigDeferred(), 0L).winGameYou()
        val picked = MatchEngine.pickOpponentFirstServer(afterGame1, Player.B2)

        // Play several more games — awaitingServePick should never be true again
        var s = picked
        repeat(8) {
            s = s.winGameYou()
            assertFalse(s.awaitingServePick, "awaitingServePick should stay false after confirmation")
        }
    }

    // -----------------------------------------------------------------------
    // Undo
    // -----------------------------------------------------------------------

    @Test fun `undo from after pick restores awaitingServePick true`() {
        // Simulate history stack: [initial, afterGame1(awaitingServePick=true), picked(false)]
        val initial = MatchEngine.initialSnapshot(doublesConfigDeferred(), 0L)
        val afterGame1 = initial.winGameYou()
        assertTrue(afterGame1.awaitingServePick)

        val picked = MatchEngine.pickOpponentFirstServer(afterGame1, Player.B2)
        assertFalse(picked.awaitingServePick)

        // "Undo" = pop picked from history → we're back to afterGame1
        assertTrue(afterGame1.awaitingServePick)
    }

    // -----------------------------------------------------------------------
    // Serve rotation after pick
    // -----------------------------------------------------------------------

    @Test fun `serve rotation is correct after pickOpponentFirstServer`() {
        // A1 serves game 1, pick B2 → order [A1, B2, A2, B1]
        val afterGame1 = MatchEngine.initialSnapshot(doublesConfigDeferred(), 0L).winGameYou()
        val picked = MatchEngine.pickOpponentFirstServer(afterGame1, Player.B2)

        // Game 2: B2 serves (index 1 in [A1, B2, A2, B1])
        assertEquals(Player.B2, picked.serve.serverPlayer)
        assertEquals(Team.OPP, picked.serve.serverTeam)

        // Game 3: A2 (index 2)
        val game3 = picked.winGameOpp()
        assertEquals(Player.A2, game3.serve.serverPlayer)
        assertEquals(Team.YOU, game3.serve.serverTeam)

        // Game 4: B1 (index 3)
        val game4 = game3.winGameYou()
        assertEquals(Player.B1, game4.serve.serverPlayer)
        assertEquals(Team.OPP, game4.serve.serverTeam)

        // Game 5: A1 (wraps back to index 0)
        val game5 = game4.winGameOpp()
        assertEquals(Player.A1, game5.serve.serverPlayer)
        assertEquals(Team.YOU, game5.serve.serverTeam)
    }
}
