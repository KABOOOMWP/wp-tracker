package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests every scoring state transition for STANDARD, GOLDEN, and STAR rule modes.
 * Covers spec sections 5, 9, and edge cases from section I.
 */
class ScoreTransitionTest {

    // -----------------------------------------------------------------------
    // Standard: normal point progression
    // -----------------------------------------------------------------------

    @Test fun `0-0 you score → 15-0`() {
        val s = makeSnapshot().you()
        assertEquals(1, s.game.youPoints)
        assertEquals(0, s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `progression to 40-0`() {
        val s = makeSnapshot().scoreMany(Team.YOU, Team.YOU, Team.YOU)
        assertEquals(3, s.game.youPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `game win at 40-0`() {
        val s = makeSnapshot().scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(1, s.set.youGames)
        assertEquals(0, s.set.oppGames)
        assertEquals(0, s.game.youPoints) // reset
        assertEquals(0, s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `40-30 full sequence - game won without deuce`() {
        // Score from 0:0: YOU YOU YOU OPP OPP → 3:2 (40-30), then YOU scores → game won
        val s = makeSnapshot()
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP)
            .you()
        assertEquals(1, s.set.youGames)
        assertEquals(0, s.game.youPoints) // reset after game win
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `40-30 you score → game won`() {
        // you=3 opp=2 → you score → 4:2 → game won (no deuce)
        val s = makeSnapshot(youPoints = 3, oppPoints = 2).you()
        assertEquals(1, s.set.youGames)
        assertEquals(GamePhase.NORMAL, s.game.phase)
        assertEquals(0, s.game.youPoints)
    }

    // -----------------------------------------------------------------------
    // Standard: deuce / advantage
    // -----------------------------------------------------------------------

    @Test fun `3-3 Standard → DEUCE`() {
        val s = makeSnapshot(youPoints = 2, oppPoints = 3).you() // → 3:3
        assertEquals(GamePhase.DEUCE, s.game.phase)
        assertEquals(3, s.game.youPoints)
        assertEquals(3, s.game.oppPoints)
    }

    @Test fun `DEUCE you score → ADV_YOU`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE).you()
        assertEquals(GamePhase.ADV_YOU, s.game.phase)
    }

    @Test fun `DEUCE opp score → ADV_OPP`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE).opp()
        assertEquals(GamePhase.ADV_OPP, s.game.phase)
    }

    @Test fun `ADV_YOU you score → game YOU`() {
        val s = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU).you()
        assertEquals(1, s.set.youGames)
        assertEquals(GamePhase.NORMAL, s.game.phase)
        assertEquals(0, s.game.youPoints)
    }

    @Test fun `ADV_YOU opp score → back to DEUCE`() {
        val s = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU).opp()
        assertEquals(GamePhase.DEUCE, s.game.phase)
    }

    @Test fun `ADV_OPP opp score → game OPP`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP).opp()
        assertEquals(1, s.set.oppGames)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `ADV_OPP you score → back to DEUCE`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP).you()
        assertEquals(GamePhase.DEUCE, s.game.phase)
    }

    @Test fun `Standard deuce can cycle multiple times`() {
        var s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        repeat(5) {
            s = s.you() // ADV_YOU
            assertEquals(GamePhase.ADV_YOU, s.game.phase)
            s = s.opp() // back to DEUCE
            assertEquals(GamePhase.DEUCE, s.game.phase)
        }
    }

    // -----------------------------------------------------------------------
    // Golden Point
    // -----------------------------------------------------------------------

    @Test fun `3-3 Golden → GOLDEN phase`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 2, oppPoints = 3
        ).you() // → 3:3
        assertEquals(GamePhase.GOLDEN, s.game.phase)
    }

    @Test fun `GOLDEN you score → game YOU`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN
        ).you()
        assertEquals(1, s.set.youGames)
        assertEquals(GamePhase.NORMAL, s.game.phase)
        assertEquals(0, s.game.youPoints)
    }

    @Test fun `GOLDEN opp score → game OPP`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.GOLDEN
        ).opp()
        assertEquals(1, s.set.oppGames)
    }

    @Test fun `Golden no DEUCE or ADV state ever`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 2, oppPoints = 3
        ).you() // 3:3 → GOLDEN
        assertNotEquals(GamePhase.DEUCE, s.game.phase)
        assertNotEquals(GamePhase.ADV_YOU, s.game.phase)
        assertNotEquals(GamePhase.ADV_OPP, s.game.phase)
    }

    // -----------------------------------------------------------------------
    // Star Point (FIP 2026)
    // -----------------------------------------------------------------------

    @Test fun `3-3 Star → DEUCE`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 2, oppPoints = 3
        ).you()
        assertEquals(GamePhase.DEUCE, s.game.phase)
        assertEquals(0, s.game.starAdvCount)
    }

    @Test fun `Star DEUCE you score → STAR_ADV_YOU`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.DEUCE, starAdvCount = 0
        ).you()
        assertEquals(GamePhase.STAR_ADV_YOU, s.game.phase)
        assertEquals(0, s.game.starAdvCount) // advCount not incremented yet
    }

    @Test fun `Star STAR_ADV_YOU you score → game YOU`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0
        ).you()
        assertEquals(1, s.set.youGames)
        assertEquals(0, s.game.starAdvCount) // reset with new game
    }

    @Test fun `Star STAR_ADV_YOU opp score → DEUCE advCount=1`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0
        ).opp()
        assertEquals(GamePhase.DEUCE, s.game.phase)
        assertEquals(1, s.game.starAdvCount)
    }

    @Test fun `Star second advantage lost → STAR_POINT`() {
        // advCount=1 → opp gets advantage → you break it → advCount=2 → STAR_POINT
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 4,
            phase = GamePhase.STAR_ADV_OPP, starAdvCount = 1
        ).you() // advantage team loses
        assertEquals(GamePhase.STAR_POINT, s.game.phase)
        assertEquals(2, s.game.starAdvCount)
    }

    @Test fun `Star STAR_POINT you score → game YOU`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2
        ).you()
        assertEquals(1, s.set.youGames)
    }

    @Test fun `Star STAR_POINT opp score → game OPP`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2
        ).opp()
        assertEquals(1, s.set.oppGames)
    }

    @Test fun `Star full pipeline from 0-0`() {
        // 40:40 → ADV_YOU (lost) → 40:40 → ADV_OPP (lost) → STAR_POINT → YOU wins
        val s = makeSnapshot(config = singlesConfig(RuleMode.STAR))
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP) // 3:3 → DEUCE
            .you()   // STAR_ADV_YOU (advCount=0)
            .opp()   // back to DEUCE (advCount=1)
            .opp()   // STAR_ADV_OPP (advCount=1)
            .you()   // STAR_POINT (advCount=2)
            .you()   // game YOU
        assertEquals(1, s.set.youGames)
    }

    @Test fun `Star starAdvCount resets after game win`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.STAR))
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP)
            .you().opp().opp().you()
            .you() // game YOU won
        assertEquals(0, s.game.starAdvCount)
    }

    // -----------------------------------------------------------------------
    // Set and match transitions
    // -----------------------------------------------------------------------

    @Test fun `win 6 games wins the set`() {
        var s = makeSnapshot()
        repeat(6) {
            s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU) // win a game
        }
        assertEquals(1, s.match.setsWonYou)
        assertEquals(0, s.set.youGames)
    }

    @Test fun `6-5 is not a set win`() {
        val s = makeSnapshot(youGames = 5, oppGames = 5).scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        // now 6:5 – not a win yet
        assertEquals(0, s.match.setsWonYou)
        assertEquals(6, s.set.youGames)
    }

    @Test fun `7-5 wins the set`() {
        val s = makeSnapshot(youGames = 6, oppGames = 5).scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(1, s.match.setsWonYou)
    }

    @Test fun `6-6 triggers tie-break`() {
        val s = makeSnapshot(youGames = 5, oppGames = 6).scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(6, s.set.youGames)
        assertEquals(6, s.set.oppGames)
    }

    @Test fun `best-of-3 match win at 2 sets`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) { // win 2 sets
            repeat(6) {
                s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
            }
        }
        assertEquals(2, s.match.setsWonYou)
        assertTrue(s.isMatchOver)
    }

    @Test fun `best-of-5 match needs 3 sets`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 5))
        repeat(2) {
            repeat(6) { s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU) }
        }
        assertFalse(s.isMatchOver)
        repeat(6) { s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU) }
        assertEquals(3, s.match.setsWonYou)
        assertTrue(s.isMatchOver)
    }

    @Test fun `no more scoring after match is over`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) { repeat(6) { s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU) } }
        assertTrue(s.isMatchOver)
        val frozen = s.you()
        assertEquals(s, frozen) // scoring on finished match returns same snapshot
    }
}
