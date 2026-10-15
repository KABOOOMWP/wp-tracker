package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Match statistics: points including tie-breaks, points won, deciders counted when the deciding point
 * is played (Golden and Star), breaks, and deuces.
 */
class StatsTest {

    private val deuce = arrayOf(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP)

    // ── Points ───────────────────────────────────────────────────────────────

    @Test fun `every point is counted and the ones you won separately`() {
        val s = makeSnapshot().you().opp().opp().you().you()
        assertEquals(5, s.stats.totalPlayedPoints)
        assertEquals(3, s.stats.pointsWonYou)
    }

    @Test fun `points carry on across games and sets`() {
        var s = makeSnapshot(config = singlesConfig(bestOf = 3))
        repeat(4) { s = s.you() }
        repeat(4) { s = s.opp() }
        assertEquals(8 to 4, s.stats.totalPlayedPoints to s.stats.pointsWonYou)
        s = s.winSetAndContinue(Team.YOU)
        assertEquals(8 + 20, s.stats.totalPlayedPoints, "20 more points finish the set 6-1")
    }

    @Test fun `tie-break points count`() {
        var s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)
        repeat(7) { s = s.you() }
        assertEquals(7, s.stats.totalPlayedPoints)
    }

    // ── Deciders ─────────────────────────────────────────────────────────────

    @Test fun `reaching the Golden Point is not a decider played`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.GOLDEN)).scoreMany(*deuce)
        assertEquals(GamePhase.GOLDEN, s.game.phase)
        assertEquals(0, s.stats.decidersPlayed)
    }

    @Test fun `a Golden Point counts when played whoever wins it`() {
        val golden = makeSnapshot(config = singlesConfig(RuleMode.GOLDEN)).scoreMany(*deuce)
        assertEquals(1 to 1, golden.you().stats.decidersPlayed to golden.you().stats.decidersWonYou)
        assertEquals(1 to 0, golden.opp().stats.decidersPlayed to golden.opp().stats.decidersWonYou)
    }

    @Test fun `a Star Point counts when played`() {
        val star = makeSnapshot(config = singlesConfig(RuleMode.STAR)).scoreMany(*deuce).you().opp().opp().you()
        assertEquals(GamePhase.STAR_POINT, star.game.phase)
        assertEquals(0, star.stats.decidersPlayed)
        assertEquals(1 to 1, star.you().stats.decidersPlayed to star.you().stats.decidersWonYou)
    }

    @Test fun `a Star advantage won outright is no decider`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.STAR)).scoreMany(*deuce).you().you()
        assertEquals(1, s.set.youGames)
        assertEquals(0, s.stats.decidersPlayed)
    }

    @Test fun `deciders accumulate over the games`() {
        var s = makeSnapshot(config = singlesConfig(RuleMode.GOLDEN))
        repeat(3) { s = s.scoreMany(*deuce).you() }
        s = s.scoreMany(*deuce).opp()
        assertEquals(4, s.stats.decidersPlayed)
        assertEquals(3, s.stats.decidersWonYou)
    }

    // ── Breaks ───────────────────────────────────────────────────────────────

    @Test fun `winning the opponent's service game is a break`() {
        val s = makeSnapshot(serverPlayer = Player.B1).scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(1 to 0, s.stats.breaksYou to s.stats.breaksOpp)
    }

    @Test fun `losing your own service game is a break for the opponent`() {
        val s = makeSnapshot(serverPlayer = Player.A1).scoreMany(Team.OPP, Team.OPP, Team.OPP, Team.OPP)
        assertEquals(0 to 1, s.stats.breaksYou to s.stats.breaksOpp)
    }

    @Test fun `holding serve is no break`() {
        val s = makeSnapshot(serverPlayer = Player.A1).scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.YOU)
        assertEquals(0 to 0, s.stats.breaksYou to s.stats.breaksOpp)
    }

    @Test fun `a tie-break win is no break`() {
        var s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK, serverPlayer = Player.B1)
        repeat(7) { s = s.you() }
        assertEquals(1, s.match.setsWon(Team.YOU))
        assertEquals(0 to 0, s.stats.breaksYou to s.stats.breaksOpp)
    }

    @Test fun `breaks accumulate over the games`() {
        val s = MatchEngine.initialSnapshot(doublesConfig(), 0)
            .winGame(Team.YOU) // A1 holds
            .winGame(Team.YOU) // B1 is broken
            .winGame(Team.OPP) // A2 is broken
        assertEquals(1 to 1, s.stats.breaksYou to s.stats.breaksOpp)
    }

    // ── Deuces ───────────────────────────────────────────────────────────────

    @Test fun `every return to 40-40 is a deuce`() {
        val s = makeSnapshot().scoreMany(*deuce)
        assertEquals(1, s.stats.deuceCount)
        assertEquals(2, s.you().opp().stats.deuceCount)
    }

    @Test fun `the Golden Point is the one deuce of its game`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.GOLDEN)).scoreMany(*deuce)
        assertEquals(1, s.stats.deuceCount)
        assertEquals(1, s.opp().stats.deuceCount, "the Golden Point ends the game")
    }

    @Test fun `all three Star deuces count including the Star Point`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.STAR)).scoreMany(*deuce).you().opp().opp().you()
        assertEquals(GamePhase.STAR_POINT, s.game.phase)
        assertEquals(3, s.stats.deuceCount)
    }

    @Test fun `level scores in a tie-break are no deuces`() {
        val s = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK).scoreMany(*deuce, Team.YOU, Team.OPP)
        assertEquals(0, s.stats.deuceCount)
    }

    @Test fun `deuces accumulate over the games`() {
        val s = makeSnapshot().scoreMany(*deuce, Team.YOU, Team.YOU).scoreMany(*deuce, Team.OPP, Team.OPP)
        assertEquals(2, s.set.youGames + s.set.oppGames)
        assertEquals(2, s.stats.deuceCount)
    }
}
