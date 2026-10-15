package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.engine.doublesConfig
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.ServeSide
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import ch.wastedpotential.padel.model.Team.OPP as O
import ch.wastedpotential.padel.model.Team.YOU as Y

/** Undo through MatchHistory restores the whole state before the point: score, phase, serve, stats and pill. */
class UndoTest {

    private fun history(config: Config = singlesConfig(), vararg points: Team) = MatchHistory().apply {
        start(config, 0)
        points.forEach { score(it) }
    }

    /** Scores [team] and undoes it; the history must be back exactly where it was. */
    private fun MatchHistory.assertUndoRestores(team: Team): Snapshot {
        val before = current!!
        val size = size
        assertNotEquals(Feedback.NONE, score(team))
        val scored = current!!
        assertEquals(Feedback.UNDO, undo())
        assertEquals(before, current)
        assertEquals(size, this.size)
        return scored
    }

    /** Twelve games won in turn: 6:6 and a tie-break, with the opponent left of the doubles pick. */
    private fun MatchHistory.reachTieBreak() {
        repeat(12) { game ->
            repeat(4) { score(if (game % 2 == 0) Y else O) }
            if (current!!.awaitingServePick) answerPrompt(0)
        }
        assertEquals(GameMode.TIEBREAK, current!!.game.mode)
    }

    @Test fun `undo a point mid-game`() {
        val scored = history().assertUndoRestores(Y)
        assertEquals(1, scored.game.youPoints)
    }

    @Test fun `undo the game-winning point restores 40-0 with the same server and side`() {
        val h = history(singlesConfig(), Y, Y, Y)
        val scored = h.assertUndoRestores(Y)
        assertEquals(1, scored.set.youGames)
        assertEquals(Player.B1, scored.serve.serverPlayer)
        assertEquals(Player.A1, h.current!!.serve.serverPlayer)
        assertEquals(ServeSide.LEFT, h.current!!.serve.serveSide)
    }

    @Test fun `undo a lost advantage restores the advantage`() {
        val h = history(singlesConfig(), Y, Y, Y, O, O, O, Y)
        assertEquals(GamePhase.ADV_YOU, h.current!!.game.phase)
        assertEquals(GamePhase.DEUCE, h.assertUndoRestores(O).game.phase)
    }

    @Test fun `undo the point that makes the Star Point restores the second advantage`() {
        val h = history(singlesConfig(RuleMode.STAR), Y, Y, Y, O, O, O, Y, O, O)
        assertEquals(GamePhase.STAR_ADV_OPP, h.current!!.game.phase)
        assertEquals(1, h.current!!.game.starAdvCount)
        val scored = h.assertUndoRestores(Y)
        assertEquals(GamePhase.STAR_POINT, scored.game.phase)
        assertEquals(3, scored.stats.deuceCount)
    }

    @Test fun `undo a won Golden Point restores the decider and its statistics`() {
        val h = history(singlesConfig(RuleMode.GOLDEN), Y, Y, Y, O, O, O)
        assertEquals(GamePhase.GOLDEN, h.current!!.game.phase)
        val scored = h.assertUndoRestores(O)
        assertEquals(1, scored.stats.decidersPlayed)
        assertEquals(0, h.current!!.stats.decidersPlayed)
    }

    @Test fun `undo a break restores the break count`() {
        val h = history(singlesConfig(), Y, Y, Y, Y, Y, Y, Y) // game 2: B1 serves, YOU at 40-0
        val scored = h.assertUndoRestores(Y)
        assertEquals(1, scored.stats.breaksYou)
        assertEquals(0, h.current!!.stats.breaksYou)
    }

    @Test fun `undo the point that starts a tie-break restores the regular game`() {
        val h = history()
        repeat(11) { game -> repeat(4) { h.score(if (game % 2 == 0) Y else O) } } // 6:5
        repeat(3) { h.score(O) }
        val scored = h.assertUndoRestores(O)
        assertEquals(GameMode.TIEBREAK, scored.game.mode)
        assertEquals(GameMode.REGULAR, h.current!!.game.mode)
        assertEquals(6 to 5, h.current!!.set.youGames to h.current!!.set.oppGames)
    }

    @Test fun `undo inside a doubles tie-break restores its server rotation`() {
        val h = MatchHistory().apply {
            start(doublesConfig(), 0)
            reachTieBreak()
        }
        repeat(2) { h.score(Y) }
        val serverAtTwo = h.current!!.serve.serverPlayer
        val scored = h.assertUndoRestores(O)
        assertNotEquals(serverAtTwo, scored.serve.serverPlayer, "the server changes after the third point")
        assertEquals(serverAtTwo, h.current!!.serve.serverPlayer)
    }

    @Test fun `undo the tie-break win restores the tie-break`() {
        val h = MatchHistory().apply {
            start(singlesConfig(), 0)
            reachTieBreak()
        }
        repeat(6) { h.score(Y) }
        val scored = h.assertUndoRestores(Y)
        assertEquals(SetScore(7, 6, true), scored.match.completedSets.single())
        assertEquals(6, h.current!!.game.youPoints)
        assertTrue(h.current!!.match.completedSets.isEmpty())
    }

    @Test fun `undo the match-winning point clears the win`() {
        val h = history()
        repeat(24) { h.score(Y) }
        h.answerPrompt(0)
        repeat(23) { h.score(Y) }
        val scored = h.assertUndoRestores(Y)
        assertTrue(scored.isMatchOver)
        assertFalse(h.current!!.isMatchOver)
    }

    @Test fun `undo down to the first snapshot and no further`() {
        val h = history(singlesConfig(), Y, O, Y, O, Y)
        val first = MatchHistory().apply { start(singlesConfig(), 0) }.current
        repeat(5) { assertEquals(Feedback.UNDO, h.undo()) }
        assertEquals(first, h.current)
        assertEquals(Feedback.NONE, h.undo())
    }
}
