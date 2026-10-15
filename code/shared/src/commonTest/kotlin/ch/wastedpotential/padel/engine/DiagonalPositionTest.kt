package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.ScoreLayout
import ch.wastedpotential.padel.model.ScorePosition
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [MatchEngine.computeScoreLayout]: an even rally (points this game) puts You bottom-right and Opp top-left,
 * an odd one You bottom-left and Opp top-right. The same in deuce, advantage and the tie-break.
 */
class DiagonalPositionTest {

    private val even = ScoreLayout(ScorePosition.BOTTOM_RIGHT, ScorePosition.TOP_LEFT)
    private val odd = ScoreLayout(ScorePosition.BOTTOM_LEFT, ScorePosition.TOP_RIGHT)

    private fun Snapshot.layout() = MatchEngine.computeScoreLayout(this)

    private fun Snapshot.layouts(points: Int): List<ScoreLayout> {
        var s = this
        return List(points) { s.layout().also { s = s.you() } }
    }

    @Test fun `the layout alternates with every point from the even one`() {
        assertEquals(listOf(even, odd, even, odd), makeSnapshot().layouts(4))
    }

    @Test fun `deuce and advantage follow the rally count`() {
        val deuce = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        assertEquals(even, deuce.layout(), "rally 6")
        assertEquals(odd, deuce.you().layout(), "advantage, rally 7")
        assertEquals(even, deuce.you().opp().layout(), "deuce again, rally 8")
        assertEquals(odd, makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP).layout())
    }

    @Test fun `deciding points follow the rally count too`() {
        assertEquals(even, makeSnapshot(singlesConfig(RuleMode.GOLDEN), youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN).layout())
        val star = makeSnapshot(singlesConfig(RuleMode.STAR), youPoints = 5, oppPoints = 5, phase = GamePhase.STAR_POINT, starAdvCount = 2)
        assertEquals(even, star.layout())
    }

    @Test fun `the tie-break alternates like a game`() {
        assertEquals(listOf(even, odd, even, odd), makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK).layouts(4))
    }

    @Test fun `a new game starts even again`() {
        val fortyThirty = makeSnapshot().scoreMany(Team.YOU, Team.OPP, Team.YOU, Team.OPP, Team.YOU)
        assertEquals(odd, fortyThirty.layout())
        val nextGame = fortyThirty.you()
        assertEquals(1, nextGame.set.youGames)
        assertEquals(even, nextGame.layout())
    }

    @Test fun `a new set starts even again`() {
        assertEquals(even, makeSnapshot().winSet(Team.YOU).layout())
        val tieBreak = makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK, youPoints = 6, oppPoints = 5)
        assertEquals(odd, tieBreak.layout())
        assertEquals(even, tieBreak.you().layout(), "7-5 wins the tie-break")
    }
}
