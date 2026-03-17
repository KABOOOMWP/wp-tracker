package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Tests tie-break start server for all four rotation slots in 2v2,
 * and for rotation continuity across set boundaries.
 *
 * Rule: TB is served by the player who would have served the next regular game
 * (i.e., the player at serveOrderIndex + 1 relative to the game that triggered 6:6).
 */
class TieBreakStartTest {

    // Helper: win a game from the given snapshot (4 uncontested points for the given team)
    private fun Snapshot.winGame(team: Team) = scoreMany(team, team, team, team)

    // -----------------------------------------------------------------------
    // 2v2: all 4 rotation starting variants
    // -----------------------------------------------------------------------
    // Trigger: makeSnapshot at 5:6 or 6:5 with specified last server, then win a game to reach 6:6.

    @Test fun `2v2 TB starts with B1 when last server was A1`() {
        // youGames=5, oppGames=6. A1 (idx=0) serves. YOU wins this game → 6:6 → TB.
        // Next in rotation after idx=0 is idx=1 (B1).
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        ).winGame(Team.YOU)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.B1, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `2v2 TB starts with A2 when last server was B1`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 6, oppGames = 5,
            serverPlayer = Player.B1, serverTeam = Team.OPP, serveOrderIndex = 1
        ).winGame(Team.OPP)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A2, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    @Test fun `2v2 TB starts with B2 when last server was A2`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A2, serverTeam = Team.YOU, serveOrderIndex = 2
        ).winGame(Team.YOU)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.B2, s.serve.serverPlayer)
        assertEquals(Team.OPP, s.serve.serverTeam)
    }

    @Test fun `2v2 TB starts with A1 when last server was B2`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 6, oppGames = 5,
            serverPlayer = Player.B2, serverTeam = Team.OPP, serveOrderIndex = 3
        ).winGame(Team.OPP)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A1, s.serve.serverPlayer)
        assertEquals(Team.YOU, s.serve.serverTeam)
    }

    // -----------------------------------------------------------------------
    // 1v1: TB starts with correct player (2-player rotation)
    // -----------------------------------------------------------------------

    @Test fun `1v1 TB starts with B1 when last server was A1`() {
        val s = makeSnapshot(
            config = singlesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        ).winGame(Team.YOU)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.B1, s.serve.serverPlayer)
    }

    @Test fun `1v1 TB starts with A1 when last server was B1`() {
        val s = makeSnapshot(
            config = singlesConfig(),
            youGames = 6, oppGames = 5,
            serverPlayer = Player.B1, serverTeam = Team.OPP, serveOrderIndex = 1
        ).winGame(Team.OPP)

        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A1, s.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // TB starts at RIGHT serve side regardless of who starts
    // -----------------------------------------------------------------------

    @Test fun `TB always starts from RIGHT serve side`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        ).winGame(Team.YOU)

        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    // -----------------------------------------------------------------------
    // TB and set boundary: rotation continuity across sets
    // -----------------------------------------------------------------------

    @Test fun `after TB-won set next set first server is one step beyond TB opener`() {
        // TB opened by A1 (serveOrderIndex=0). TB won by YOU.
        // After set: serve advances to (0+1)%4 = 1 → B1.
        val tbState = makeSnapshot(
            config = doublesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6, oppPoints = 0,
            serverPlayer = Player.B2,  // current TB server at point 6 per rotation formula
            serverTeam = Team.OPP,
            serveOrderIndex = 0        // A1 opened the TB
        )
        val afterTB = tbState.you()  // 7:0 → YOU wins TB → set 7:6 → YOU wins set

        assertEquals(1, afterTB.match.setsWonYou)
        assertEquals(Player.B1, afterTB.serve.serverPlayer)  // (0+1)%4 = B1
        assertEquals(Team.OPP, afterTB.serve.serverTeam)
    }

    @Test fun `after TB-won set new set games continue rotation correctly`() {
        // TB opened by A1 (idx=0). Set won. Next set starts with B1 (idx=1).
        // B1 serves first game of set 2. After winning that game, A2 (idx=2) should serve.
        val tbState = makeSnapshot(
            config = doublesConfig(),
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6, oppPoints = 0,
            serverTeam = Team.OPP,
            serveOrderIndex = 0
        )
        val set2Start = tbState.you()  // win TB → new set, B1 (idx=1) serves
        assertEquals(Player.B1, set2Start.serve.serverPlayer)

        // Confirm position switches (both teams keep positions) before play resumes
        val ready = MatchEngine.confirmYouPositionSwitch(set2Start, false)
        val ready2 = MatchEngine.confirmOppPositionSwitch(ready, false)

        val afterFirstGame = ready2.winGame(Team.YOU)  // B1 served, game won → A2 next
        assertEquals(Player.A2, afterFirstGame.serve.serverPlayer)
    }

    // -----------------------------------------------------------------------
    // TB game mode state
    // -----------------------------------------------------------------------

    @Test fun `entering TB resets game points to 0`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        ).winGame(Team.YOU)

        assertEquals(0, s.game.youPoints)
        assertEquals(0, s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `entering TB sets games to 6-6`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 5, oppGames = 6,
            serverPlayer = Player.A1, serverTeam = Team.YOU, serveOrderIndex = 0
        ).winGame(Team.YOU)

        assertEquals(6, s.set.youGames)
        assertEquals(6, s.set.oppGames)
    }
}
