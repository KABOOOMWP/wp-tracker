package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every scoring transition of a game in the Standard, Golden and Star rule modes, and of a set. */
class ScoreTransitionTest {

    private fun points(s: Snapshot) = s.game.youPoints to s.game.oppPoints

    // ── Standard: points and game ────────────────────────────────────────────

    @Test fun `points add up to the game`() {
        val s = makeSnapshot().you()
        assertEquals(1 to 0, points(s))
        assertEquals(GamePhase.NORMAL, s.game.phase)
        val won = s.scoreMany(Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.YOU) // 40-30, then the game
        assertEquals(1 to 0, won.set.youGames to won.set.oppGames)
        assertEquals(0 to 0, points(won))
        assertEquals(GamePhase.NORMAL, won.game.phase)
    }

    // ── Standard: deuce and advantage ────────────────────────────────────────

    @Test fun `40-40 is deuce`() {
        val s = makeSnapshot(youPoints = 2, oppPoints = 3).you()
        assertEquals(GamePhase.DEUCE, s.game.phase)
        assertEquals(3 to 3, points(s))
    }

    @Test fun `the point after deuce is an advantage`() {
        val deuce = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        assertEquals(GamePhase.ADV_YOU, deuce.you().game.phase)
        assertEquals(GamePhase.ADV_OPP, deuce.opp().game.phase)
    }

    @Test fun `an advantage wins the game or falls back to deuce`() {
        val advYou = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU)
        assertEquals(1, advYou.you().set.youGames)
        assertEquals(GamePhase.DEUCE, advYou.opp().game.phase)
        val advOpp = makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP)
        assertEquals(1, advOpp.opp().set.oppGames)
        assertEquals(GamePhase.DEUCE, advOpp.you().game.phase)
    }

    @Test fun `deuce and advantage cycle without limit`() {
        var s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        repeat(5) {
            s = s.you()
            assertEquals(GamePhase.ADV_YOU, s.game.phase)
            s = s.opp()
            assertEquals(GamePhase.DEUCE, s.game.phase)
        }
        assertEquals(0, s.set.youGames + s.set.oppGames)
    }

    // ── Golden Point ─────────────────────────────────────────────────────────

    @Test fun `40-40 is the Golden Point and the next point wins`() {
        val golden = makeSnapshot(config = singlesConfig(RuleMode.GOLDEN), youPoints = 2, oppPoints = 3).you()
        assertEquals(GamePhase.GOLDEN, golden.game.phase)
        assertEquals(1, golden.you().set.youGames)
        assertEquals(1, golden.opp().set.oppGames)
        assertEquals(GamePhase.NORMAL, golden.you().game.phase)
    }

    // ── Star Point ───────────────────────────────────────────────────────────

    @Test fun `40-40 is deuce and the first advantage is a Star advantage`() {
        val deuce = makeSnapshot(config = singlesConfig(RuleMode.STAR), youPoints = 2, oppPoints = 3).you()
        assertEquals(GamePhase.DEUCE, deuce.game.phase)
        assertEquals(0, deuce.game.starAdvCount)
        assertEquals(GamePhase.STAR_ADV_YOU, deuce.you().game.phase)
        assertEquals(GamePhase.STAR_ADV_OPP, deuce.opp().game.phase)
    }

    @Test fun `a Star advantage wins the game or counts as lost`() {
        val adv = makeSnapshot(config = singlesConfig(RuleMode.STAR), youPoints = 4, oppPoints = 3, phase = GamePhase.STAR_ADV_YOU)
        assertEquals(1, adv.you().set.youGames)
        val lost = adv.opp()
        assertEquals(GamePhase.DEUCE, lost.game.phase)
        assertEquals(1, lost.game.starAdvCount)
    }

    @Test fun `the second lost advantage makes the Star Point which the next point wins`() {
        val star = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3,
            oppPoints = 4,
            phase = GamePhase.STAR_ADV_OPP,
            starAdvCount = 1,
        )
            .you()
        assertEquals(GamePhase.STAR_POINT, star.game.phase)
        assertEquals(2, star.game.starAdvCount)
        assertEquals(1, star.you().set.youGames)
        assertEquals(1, star.opp().set.oppGames)
    }

    @Test fun `a Star game played from love resets the lost advantages for the next game`() {
        val s = makeSnapshot(config = singlesConfig(RuleMode.STAR))
            .scoreMany(Team.YOU, Team.YOU, Team.YOU, Team.OPP, Team.OPP, Team.OPP) // deuce
            .you().opp() // advantage lost
            .opp().you() // advantage lost: Star Point
            .you()
        assertEquals(1, s.set.youGames)
        assertEquals(0, s.game.starAdvCount)
    }

    // ── Sets ─────────────────────────────────────────────────────────────────

    @Test fun `six games win the set and the next starts at love`() {
        var s = makeSnapshot()
        repeat(6) { s = s.winGame(Team.YOU) }
        assertEquals(1, s.match.setsWon(Team.YOU))
        assertEquals(0 to 0, s.set.youGames to s.set.oppGames)
    }

    @Test fun `a set needs a lead of two games or the tie-break`() {
        val sixFive = makeSnapshot(youGames = 5, oppGames = 5).winGame(Team.YOU)
        assertEquals(0, sixFive.match.setsWon(Team.YOU))
        assertEquals(1, sixFive.winGame(Team.YOU).match.setsWon(Team.YOU), "7-5")
        assertEquals(GameMode.TIEBREAK, sixFive.winGame(Team.OPP).game.mode, "6-6")
    }
}
