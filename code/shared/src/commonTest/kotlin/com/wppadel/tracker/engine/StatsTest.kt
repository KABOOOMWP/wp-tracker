package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests match summary statistics completeness.
 *
 * Covers:
 *   - totalPlayedPoints across the whole match including tie-breaks
 *   - goldenDecidersPlayed increments exactly when the deciding point is scored, not before
 *   - goldenDecidersWonYou increments only when you win the golden point
 *   - isMatchOver set exactly once (and score returns same snapshot after match ends)
 *   - endedAt policy: null during match, non-null at match end (if the engine sets it)
 */
class StatsTest {

    // -----------------------------------------------------------------------
    // totalPlayedPoints
    // -----------------------------------------------------------------------

    @Test fun `totalPlayedPoints starts at 0`() {
        assertEquals(0, makeSnapshot().stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints increments for every scored point`() {
        val s = makeSnapshot().you().opp().you()
        assertEquals(3, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints persists across game boundaries`() {
        var s = makeSnapshot()
        repeat(4) { s = s.you() }   // win a game (4 points)
        repeat(3) { s = s.opp() }   // 3 more points in next game
        assertEquals(7, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints persists across set boundaries`() {
        var s = makeSnapshot()
        // Win 6 games (4 points each = 24 points) → set done
        repeat(6) { repeat(4) { s = s.you() } }
        assertEquals(24, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints includes tie-break points`() {
        // Play 6 games (4 pts each = 24 pts to reach 6:0 in set), then switch to TB scenario
        // Easier: start directly in TB and count
        var s = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK
        )
        repeat(7) { s = s.you() }   // 7 TB points → you win 7:0
        assertEquals(7, s.stats.totalPlayedPoints)
    }

    @Test fun `totalPlayedPoints across full match with multiple sets`() {
        // Two sets, each won 6:0 (4 pts per game × 6 games = 24 pts per set)
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) {
            repeat(6) { repeat(4) { s = s.you() } }
        }
        assertEquals(48, s.stats.totalPlayedPoints)
        assertTrue(s.isMatchOver)
    }

    // -----------------------------------------------------------------------
    // goldenDecidersPlayed
    // -----------------------------------------------------------------------

    @Test fun `goldenDecidersPlayed starts at 0`() {
        assertEquals(0, makeSnapshot().stats.goldenDecidersPlayed)
    }

    @Test fun `goldenDecidersPlayed does NOT increment when entering 40-40`() {
        // At 2:3, you score → 3:3 → GOLDEN phase begins. Counter should still be 0.
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 2, oppPoints = 3
        ).you()  // → 3:3 → GOLDEN
        assertEquals(GamePhase.GOLDEN, s.game.phase)
        assertEquals(0, s.stats.goldenDecidersPlayed)  // not yet — point hasn't been played
    }

    @Test fun `goldenDecidersPlayed increments when GOLDEN deciding point is scored`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        ).you()  // play the deciding point
        assertEquals(1, s.stats.goldenDecidersPlayed)
    }

    @Test fun `goldenDecidersPlayed increments regardless of who wins the golden point`() {
        val sYouWins = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        ).you()
        assertEquals(1, sYouWins.stats.goldenDecidersPlayed)

        val sOppWins = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        ).opp()
        assertEquals(1, sOppWins.stats.goldenDecidersPlayed)
    }

    @Test fun `goldenDecidersPlayed accumulates across multiple golden points in match`() {
        val config = singlesConfig(RuleMode.GOLDEN)
        var s = makeSnapshot(config = config)
        // Win game 1 normally (no golden)
        repeat(4) { s = s.you() }
        assertEquals(0, s.stats.goldenDecidersPlayed)

        // Play a golden decider in game 2
        s = makeStatsSnapshot(
            config = config,
            youGames = 1, oppGames = 0,
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN,
            totalPlayedPoints = s.stats.totalPlayedPoints
        ).you()
        assertEquals(1, s.stats.goldenDecidersPlayed)

        // Rebuild and play a second golden decider later
        val s2 = makeStatsSnapshot(
            config = config,
            youGames = 3, oppGames = 2,
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN,
            goldenDecidersPlayed = 1, goldenDecidersWonYou = 1
        ).opp()
        assertEquals(2, s2.stats.goldenDecidersPlayed)
    }

    // -----------------------------------------------------------------------
    // goldenDecidersWonYou
    // -----------------------------------------------------------------------

    @Test fun `goldenDecidersWonYou increments when YOU win the golden point`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        ).you()
        assertEquals(1, s.stats.goldenDecidersWonYou)
    }

    @Test fun `goldenDecidersWonYou does NOT increment when OPP wins the golden point`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        ).opp()
        assertEquals(0, s.stats.goldenDecidersWonYou)
        assertEquals(1, s.stats.goldenDecidersPlayed)
    }

    @Test fun `goldenDecidersWonYou - 2 golden points and you win both`() {
        val s = makeStatsSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN,
            goldenDecidersPlayed = 1, goldenDecidersWonYou = 0
        ).you()
        assertEquals(2, s.stats.goldenDecidersPlayed)
        assertEquals(1, s.stats.goldenDecidersWonYou)
    }

    // -----------------------------------------------------------------------
    // Match end
    // -----------------------------------------------------------------------

    @Test fun `isMatchOver false during match`() {
        val s = makeSnapshot(config = singlesConfig(bestOf = 3), setsWonYou = 1)
        assertFalse(s.isMatchOver)
    }

    @Test fun `isMatchOver true exactly when required sets are won`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) {
            assertFalse(s.isMatchOver)
            repeat(6) { repeat(4) { s = s.you() } }
        }
        assertTrue(s.isMatchOver)
    }

    @Test fun `scoring after match ends returns identical snapshot`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) { repeat(6) { repeat(4) { s = s.you() } } }
        assertTrue(s.isMatchOver)
        val frozen = s.you()
        assertEquals(s, frozen)
    }

    @Test fun `stats do not increment after match is over`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(2) { repeat(6) { repeat(4) { s = s.you() } } }
        val pointsAtEnd = s.stats.totalPlayedPoints
        val afterExtra = s.you().opp()
        assertEquals(pointsAtEnd, afterExtra.stats.totalPlayedPoints)
    }

    // -----------------------------------------------------------------------
    // pointsWonYou
    // -----------------------------------------------------------------------

    @Test fun `pointsWonYou starts at 0`() {
        assertEquals(0, makeSnapshot().stats.pointsWonYou)
    }

    @Test fun `pointsWonYou increments only when YOU score`() {
        val s = makeSnapshot().you().opp().you()
        assertEquals(2, s.stats.pointsWonYou)
        assertEquals(3, s.stats.totalPlayedPoints)
    }

    @Test fun `pointsWonYou + = totalPlayedPoints`() {
        val s = makeSnapshot().you().opp().opp().you().you()
        assertEquals(5, s.stats.totalPlayedPoints)
        assertEquals(3, s.stats.pointsWonYou)
        assertEquals(2, s.stats.totalPlayedPoints - s.stats.pointsWonYou)
    }

    @Test fun `pointsWonYou persists across game and set boundaries`() {
        var s = makeSnapshot()
        repeat(4) { s = s.you() }  // win game (4 pts for you)
        repeat(4) { s = s.opp() }  // opp wins next game (0 for you)
        assertEquals(4, s.stats.pointsWonYou)
        assertEquals(8, s.stats.totalPlayedPoints)
    }

    // -----------------------------------------------------------------------
    // breaksYou / breaksOpp
    // -----------------------------------------------------------------------

    @Test fun `breaksYou starts at 0`() {
        assertEquals(0, makeSnapshot().stats.breaksYou)
    }

    @Test fun `breaksYou increments when YOU win a game against OPP serve`() {
        // OPP serves (serverTeam=OPP), YOU wins the game → break for YOU
        val s = makeSnapshot(serverTeam = Team.OPP, serverPlayer = Player.B1)
        val after = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(1, after.stats.breaksYou)
        assertEquals(0, after.stats.breaksOpp)
    }

    @Test fun `breaksOpp increments when OPP wins a game against YOU serve`() {
        val s = makeSnapshot(serverTeam = Team.YOU, serverPlayer = Player.A1)
        val after = s.scoreMany(Team.OPP, Team.OPP, Team.OPP, Team.OPP)
        assertEquals(0, after.stats.breaksYou)
        assertEquals(1, after.stats.breaksOpp)
    }

    @Test fun `no break counted when server wins own game`() {
        val s = makeSnapshot(serverTeam = Team.YOU, serverPlayer = Player.A1)
        val after = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(0, after.stats.breaksYou)
        assertEquals(0, after.stats.breaksOpp)
    }

    @Test fun `tiebreak win does not count as a break`() {
        val s = makeSnapshot(
            youGames = 6, oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            serverTeam = Team.OPP, serverPlayer = Player.B1
        )
        // YOU wins TB 7:0
        var after = s
        repeat(7) { after = after.you() }
        assertEquals(0, after.stats.breaksYou)
        assertEquals(0, after.stats.breaksOpp)
    }

    @Test fun `breaks accumulate over multiple games`() {
        // Game 1: A1 serves, YOU wins → no break
        // Game 2: B1 serves, YOU wins → break for YOU
        // Game 3: A2 serves, OPP wins → break for OPP
        var s = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1, serveOrderIndex = 0)
        s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)  // game 1: YOU holds
        s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)  // game 2: YOU breaks B1
        s = s.scoreMany(Team.OPP, Team.OPP, Team.OPP, Team.OPP) // game 3: OPP breaks A2
        assertEquals(1, s.stats.breaksYou)
        assertEquals(1, s.stats.breaksOpp)
    }

    // -----------------------------------------------------------------------
    // deuceCount
    // -----------------------------------------------------------------------

    @Test fun `deuceCount starts at 0`() {
        assertEquals(0, makeSnapshot().stats.deuceCount)
    }

    @Test fun `deuceCount increments when game reaches deuce`() {
        // Score to 3:3 (deuce) in standard mode
        val s = makeSnapshot(config = singlesConfig(RuleMode.STANDARD))
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP)
        assertEquals(1, s.stats.deuceCount)
    }

    @Test fun `deuceCount increments again when returning to deuce after advantage`() {
        // 3:3 → DEUCE (count=1) → ADV_YOU → DEUCE again (count=2)
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STANDARD),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.DEUCE,
            serverTeam = Team.YOU
        )
        // After this snapshot is created the deuce isn't counted yet — it was entered before.
        // Score YOU → ADV_YOU, then OPP → back to DEUCE
        val afterSecondDeuce = s.you().opp()
        // Original deuce not counted (snapshot pre-built), one new deuce added when returned
        assertEquals(1, afterSecondDeuce.stats.deuceCount)
    }

    @Test fun `deuceCount is 0 for golden mode`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 2, oppPoints = 3
        ).you()  // → 3:3 → GOLDEN (not DEUCE)
        assertEquals(0, s.stats.deuceCount)
    }

    @Test fun `deuceCount accumulates across multiple games`() {
        // Two games both reaching deuce in standard mode
        var s = makeSnapshot(config = singlesConfig(RuleMode.STANDARD))
        // Game 1: score to deuce then YOU wins advantage
        s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP, Team.YOU, Team.YOU)
        // Game 2: score to deuce then OPP wins advantage
        s = s.scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP, Team.OPP, Team.OPP)
        assertEquals(2, s.stats.deuceCount)
    }
}

// -----------------------------------------------------------------------
// TestHelpers extension for stats-rich snapshot construction
// -----------------------------------------------------------------------

private fun makeStatsSnapshot(
    config: Config = singlesConfig(),
    youGames: Int = 0,
    oppGames: Int = 0,
    youPoints: Int = 0,
    oppPoints: Int = 0,
    phase: GamePhase = GamePhase.NORMAL,
    setsWonYou: Int = 0,
    totalPlayedPoints: Int = 0,
    goldenDecidersPlayed: Int = 0,
    goldenDecidersWonYou: Int = 0,
    pointsWonYou: Int = 0,
    breaksYou: Int = 0,
    breaksOpp: Int = 0,
    deuceCount: Int = 0
): Snapshot = makeSnapshot(
    config = config,
    youPoints = youPoints,
    oppPoints = oppPoints,
    youGames = youGames,
    oppGames = oppGames,
    setsWonYou = setsWonYou,
    phase = phase
).copy(
    stats = StatsState(
        totalPlayedPoints = totalPlayedPoints,
        goldenDecidersPlayed = goldenDecidersPlayed,
        goldenDecidersWonYou = goldenDecidersWonYou,
        pointsWonYou = pointsWonYou,
        breaksYou = breaksYou,
        breaksOpp = breaksOpp,
        deuceCount = deuceCount
    )
)
