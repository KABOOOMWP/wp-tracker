package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Edge-case pill detection tests not covered by the main PillDetectionTest.
 *
 * Covers:
 *   - Break point during Golden / Star decider (priority: Break > Golden/Star)
 *   - Set point at 6:5 specifically (serving and returning variants)
 *   - Match point in best-of-5 at various set-won counts
 *   - Pill hidden at 5:5 (no set point yet)
 */
class PillDetectionEdgeCaseTest {

    // -----------------------------------------------------------------------
    // Break point during deciding-point phases
    // Priority spec: Break Point > Star/Golden  ← spec §10
    // -----------------------------------------------------------------------

    @Test fun `Golden phase opp serves you return → GOLDEN_POINT`() {
        // OPP serves. GOLDEN phase → deciding point. GOLDEN_POINT takes priority over break.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.GOLDEN_POINT, MatchEngine.computePill(s))
    }

    @Test fun `STAR_POINT opp serves you return → STAR_POINT`() {
        // Same rule: deciding STAR_POINT overrides break label.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.STAR_POINT, MatchEngine.computePill(s))
    }

    @Test fun `Golden phase you serve → GOLDEN_POINT`() {
        // YOU serve. GOLDEN phase → GOLDEN_POINT shown regardless of who serves.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.GOLDEN_POINT, MatchEngine.computePill(s))
    }

    @Test fun `STAR_ADV_YOU you serve opp returning - HIDDEN`() {
        // YOU have advantage and YOU serve. OPP is returning but OPP cannot win in STAR_ADV_YOU.
        // No break → HIDDEN. (STAR_ADV phases are not a pill state.)
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // Set point at 6:5
    // -----------------------------------------------------------------------

    @Test fun `6-5 games and you have game point while serving → SET_POINT`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 5,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Winning this game → 7:5 → set win for you
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `6-5 games and you returning with game point → SET_POINT also break but set wins priority`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 5,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.OPP   // opp serves → you returning → also break point
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `5-6 games and opp has game point → SET_POINT for opp`() {
        val s = makeSnapshot(
            youGames = 5, oppGames = 6,
            youPoints = 0, oppPoints = 3,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `5-5 games and you have game point → NOT set point 6-5 not enough`() {
        val s = makeSnapshot(
            youGames = 5, oppGames = 5,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Winning → 6:5, need one more game for 7:5 → not set point yet
        assertNotEquals(PillState.SET_POINT, MatchEngine.computePill(s))
        assertNotEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // Match point in best-of-5
    // -----------------------------------------------------------------------

    @Test fun `best-of-5 at 2-0 sets and set point → MATCH_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 2, setsWonOpp = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Win game → 6:0 → win set → 3:0 sets → match won (3 of 5)
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `best-of-5 at 2-1 sets and set point → MATCH_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 2, setsWonOpp = 1,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    @Test fun `best-of-5 at 1-0 sets and set point → SET_POINT not match point`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 1, setsWonOpp = 0,
            youGames = 5, oppGames = 0,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.YOU
        )
        // Win set → 2:0, not enough for best-of-5 (need 3)
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `best-of-5 at 1-1 sets and returning and set point → SET_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 1, setsWonOpp = 1,
            youGames = 5, oppGames = 2,
            youPoints = 3, oppPoints = 0,
            serverTeam = Team.OPP   // returning → also break
        )
        assertEquals(PillState.SET_POINT, MatchEngine.computePill(s))
    }

    @Test fun `best-of-5 opp at 2 sets with match point → MATCH_POINT`() {
        val s = makeSnapshot(
            config = singlesConfig(bestOf = 5),
            setsWonYou = 0, setsWonOpp = 2,
            youGames = 0, oppGames = 5,
            youPoints = 0, oppPoints = 3,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.MATCH_POINT, MatchEngine.computePill(s))
    }

    // -----------------------------------------------------------------------
    // Pill hidden: Deuce / normal play, no special moment
    // -----------------------------------------------------------------------

    @Test fun `15-0 in middle of set → HIDDEN`() {
        val s = makeSnapshot(youGames = 2, oppGames = 3, youPoints = 1, oppPoints = 0)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `30-30 → HIDDEN`() {
        val s = makeSnapshot(youPoints = 2, oppPoints = 2)
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `STAR_ADV_YOU not at deciding point → HIDDEN`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }

    @Test fun `STAR_ADV_OPP opp serves you returning - HIDDEN`() {
        // OPP serves their service game at STAR_ADV_OPP. YOU return but cannot win → HIDDEN.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 4,
            phase = GamePhase.STAR_ADV_OPP, starAdvCount = 1,
            serverTeam = Team.OPP
        )
        assertEquals(PillState.HIDDEN, MatchEngine.computePill(s))
    }
}
