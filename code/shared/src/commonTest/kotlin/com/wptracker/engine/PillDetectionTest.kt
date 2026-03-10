package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Tests status-pill computation for all pill states and priority ordering.
 * Covers spec sections 10, 11, H, and D3.
 *
 * Priority (highest first):
 *   MATCH_POINT > SET_POINT > BREAK_POINT > STAR_POINT / GOLDEN_POINT > TIEBREAK
 */
class PillDetectionTest {

    // -----------------------------------------------------------------------
    // HIDDEN: normal play, deuce, advantage
    // -----------------------------------------------------------------------

    @Test fun `normal game → HIDDEN`() {
        val s = makeSnapshot(youPoints = 0, oppPoints = 0)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `deuce state → HIDDEN`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `ADV_YOU → HIDDEN`() {
        val s = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `ADV_OPP opp serves you returning - HIDDEN (returner has no game point)`() {
        // OPP serves their service game at advantage. YOU are returning but cannot win → HIDDEN.
        val s = makeSnapshot(
            youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // TIEBREAK
    // -----------------------------------------------------------------------

    @Test fun `tie-break game → TIEBREAK pill`() {
        val s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)
        assertEquals(PillState.TIEBREAK, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // GOLDEN_POINT
    // -----------------------------------------------------------------------

    @Test fun `Golden phase → GOLDEN_POINT pill (overrides break)`() {
        // GOLDEN phase is the deciding moment — GOLDEN_POINT has higher priority than BREAK_POINT.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.GOLDEN_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // STAR_POINT
    // -----------------------------------------------------------------------

    @Test fun `Star phase STAR_POINT → STAR_POINT pill (overrides break)`() {
        // STAR_POINT is the deciding moment — STAR_POINT has higher priority than BREAK_POINT.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.STAR_POINT, MatchEngine.computePill(s))
    }

    @Test fun `Star ADV phases (not deciding) - HIDDEN`() {
        // STAR_ADV_YOU: you serve, opp returns but opp cannot win next point → no break → HIDDEN.
        val s1 = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            phase = GamePhase.STAR_ADV_YOU,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s1))

        // STAR_ADV_OPP: opp serves their service game at advantage. YOU return but cannot win → HIDDEN.
        val s2 = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            phase = GamePhase.STAR_ADV_OPP,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s2))
    }

    // -----------------------------------------------------------------------
    // BREAK_POINT
    // -----------------------------------------------------------------------

    // Break point: the returning team (non-server) can win the current game with the next point.
    // Returning team has game point.

    @Test fun `YOU returning, opp at 40-0 → not break point (YOU has no game point)`() {
        // Opp serves, opp has 3, you have 0 → opp has game point, not you → no break for you
        val s = makeSnapshot(
            youPoints = 0, oppPoints = 3,
            serverTeam = Team.OPP // opp serves, you return
        )
        // Opp has game point, but opp is the server → this is NOT a break point
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `YOU returning opp serves, YOU has 40-0 → BREAK_POINT`() {
        // Opp serves (you return). You have 3, opp has 0. You can win → break point.
        val s = makeSnapshot(
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.OPP // opp serves
        )
        assertEquals(PillState.BREAK_POINT, MatchEngine.computePill(s))
    }

    @Test fun `YOU serves, YOU has game point → NOT break point (it's YOU's serve)`() {
        val s = makeSnapshot(
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // YOU can win the game, but YOU is the server → this is a game point, not a break point.
        // The pill shows nothing special here unless it's a set/match point.
        assertNotEquals(PillState.BREAK_POINT, MatchEngine.computePill(s))
    }

    @Test fun `ADV_OPP you serve opp returning → BREAK_POINT`() {
        // YOU serve. OPP is returning and has ADV_OPP → OPP can win the game → break point for OPP.
        val s = makeSnapshot(
            youPoints = 3, oppPoints = 4,
            phase = GamePhase.ADV_OPP,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.BREAK_POINT, MatchEngine.computePill(s))
    }

    @Test fun `ADV_YOU opp serves you returning → BREAK_POINT`() {
        // OPP serves. YOU are returning and have ADV_YOU → YOU can win the game → break point for YOU.
        // Classic scenario: you broke through deuce and are now at advantage on return.
        val s = makeSnapshot(
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.ADV_YOU,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.BREAK_POINT, MatchEngine.computePill(s))
    }

    @Test fun `30-40 you returning opp serves → BREAK_POINT`() {
        // Standard game point for the returner: opp serves, you have 3 points vs opp's 2 → BREAK_POINT.
        // (Equivalent to 40-30 from returner's perspective.)
        val s = makeSnapshot(
            youPoints = 3, oppPoints = 2,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.BREAK_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // SET_POINT
    // -----------------------------------------------------------------------

    @Test fun `you have set point at 5-3 games and winning game point`() {
        // you have 5 games, opp has 3. You have game point (3:0) → winning this game → 6:3 → set win
        val s = makeSnapshot(
            youGames = 5, oppGames = 3,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `opp has set point at 3-5 games, opp serving with game point`() {
        val s = makeSnapshot(
            youGames = 3, oppGames = 5,
            youPoints = 0, oppPoints = 3,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `5-5 games no set point yet`() {
        val s = makeSnapshot(
            youGames = 5, oppGames = 5,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Winning this game → 6:5 → not a set win yet (need 7:5)
        assertNotEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `6-5 games and game point → SET_POINT`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 5,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // MATCH_POINT
    // -----------------------------------------------------------------------

    @Test fun `best-of-3 match point at 1 set won and set point`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1, setsWonOpp = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `1 set each → no match point`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1, setsWonOpp = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Winning this set → 2:1 → match win → MATCH_POINT
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `best-of-5 match point only at 2 sets won`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 1, setsWonOpp = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Winning this set → 2:0 → not enough for best-of-5
        assertNotEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // Priority ordering
    // -----------------------------------------------------------------------

    @Test fun `MATCH_POINT beats SET_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `MATCH_POINT beats BREAK_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.OPP // you are returning → also a break point
        )
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `SET_POINT beats BREAK_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.OPP // returning → break point too
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `TIEBREAK is lowest priority, beat by MATCH_POINT`() {
        // If it's tie-break AND match point, show MATCH_POINT
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 3),
            setsWonYou = 1,
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6, oppPoints = 5,
            serverTeam = Team.YOU
        )
        // You can win the TB → win the set → win the match
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }
}
