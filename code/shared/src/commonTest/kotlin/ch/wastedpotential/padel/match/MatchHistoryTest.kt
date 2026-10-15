package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.engine.doublesConfig
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MatchHistoryTest {

    private fun started(config: Config = singlesConfig()) = MatchHistory().apply { start(config, 1_000) }

    private fun MatchHistory.scoreTimes(team: Team, times: Int) = repeat(times) { score(team) }

    /** Wins a 6-0 set for [team] in singles and acknowledges the side change. */
    private fun MatchHistory.winSet(team: Team) {
        scoreTimes(team, 24)
        if (current!!.awaitingCourtSideChange) answerPrompt(0)
    }

    /** A won singles match, summary not shown yet. */
    private fun won() = started().apply {
        winSet(Team.YOU)
        winSet(Team.YOU)
    }

    // ── Screens ──────────────────────────────────────────────────────────────

    @Test fun `empty history shows setup`() {
        val h = MatchHistory()
        assertEquals(AppScreen.SETUP, h.screen)
        assertNull(h.current)
        assertFalse(h.canUndo)
    }

    @Test fun `start shows the match with one snapshot`() {
        val h = started()
        assertEquals(AppScreen.MATCH, h.screen)
        assertEquals(1, h.size)
        assertEquals(1_000, h.current!!.match.startedAt)
    }

    @Test fun `start again resets the history`() {
        val h = started()
        h.scoreTimes(Team.YOU, 3)
        h.start(singlesConfig(), 2_000)
        assertEquals(1, h.size)
        assertEquals(2_000, h.current!!.match.startedAt)
    }

    @Test fun `clear returns to setup`() {
        val h = started()
        h.clear()
        assertEquals(AppScreen.SETUP, h.screen)
        assertNull(h.current)
    }

    // ── Scoring feedback ─────────────────────────────────────────────────────

    @Test fun `score reports the point for each team`() {
        val h = started()
        assertEquals(Feedback.POINT_YOU, h.score(Team.YOU))
        assertEquals(Feedback.POINT_OPP, h.score(Team.OPP))
        assertEquals(3, h.size)
    }

    @Test fun `game set and match winning points report GAME_WON`() {
        val h = started()
        h.scoreTimes(Team.YOU, 3)
        assertEquals(Feedback.GAME_WON, h.score(Team.YOU))
        h.scoreTimes(Team.YOU, 19)
        assertEquals(Feedback.GAME_WON, h.score(Team.YOU), "set point")
        h.answerPrompt(0)
        h.scoreTimes(Team.YOU, 23)
        assertEquals(Feedback.GAME_WON, h.score(Team.YOU), "match point")
        assertTrue(h.current!!.isMatchOver)
    }

    @Test fun `score before start does nothing`() {
        assertEquals(Feedback.NONE, MatchHistory().score(Team.YOU))
    }

    @Test fun `score is blocked while the serve pick is open`() {
        val h = started(doublesConfig())
        h.scoreTimes(Team.YOU, 4)
        assertTrue(h.current!!.awaitingServePick)
        val size = h.size
        assertEquals(Feedback.NONE, h.score(Team.YOU))
        assertEquals(size, h.size)
    }

    @Test fun `score is blocked once the match is won`() {
        val h = won()
        assertEquals(Feedback.NONE, h.score(Team.YOU))
    }

    // ── Undo ─────────────────────────────────────────────────────────────────

    @Test fun `undo removes the last point`() {
        val h = started()
        h.score(Team.YOU)
        assertEquals(Feedback.UNDO, h.undo())
        assertEquals(0, h.current!!.game.youPoints)
    }

    @Test fun `undo at the start reports nothing undone`() {
        val h = started()
        assertEquals(Feedback.NONE, h.undo())
        assertEquals(1, h.size)
    }

    // ── Prompts ──────────────────────────────────────────────────────────────

    @Test fun `the serve pick is pushed so undo shows the picker again`() {
        val h = started(doublesConfig())
        h.scoreTimes(Team.YOU, 4)
        assertEquals(Feedback.CONFIRM, h.answerPrompt(0))
        assertEquals(Player.B2, h.current!!.serve.serverPlayer, "left is the opponent on the left")
        assertFalse(h.current!!.awaitingServePick)
        h.undo()
        assertTrue(h.current!!.awaitingServePick)
    }

    @Test fun `the serve pick on the right picks the right-side opponent`() {
        val h = started(doublesConfig())
        h.scoreTimes(Team.YOU, 4)
        h.answerPrompt(1)
        assertEquals(Player.B1, h.current!!.serve.serverPlayer)
    }

    @Test fun `the side change replaces the top so undo skips it`() {
        val h = started()
        h.scoreTimes(Team.YOU, 24)
        val sizeAtSetEnd = h.size
        assertEquals(Feedback.SIDE_CHANGE, h.answerPrompt(0))
        assertEquals(sizeAtSetEnd, h.size)
        assertFalse(h.current!!.awaitingCourtSideChange)
        h.undo()
        assertEquals(5, h.current!!.set.youGames)
    }

    @Test fun `doubles answers the side change then both position switches in place`() {
        val h = started(doublesConfig())
        h.scoreTimes(Team.YOU, 4)
        h.answerPrompt(0)
        h.scoreTimes(Team.YOU, 20)
        val size = h.size
        assertEquals(Feedback.SIDE_CHANGE, h.answerPrompt(0))
        assertTrue(h.current!!.awaitingYouPositionSwitch)
        assertEquals(Feedback.CONFIRM, h.answerPrompt(0)) // YOU switch
        assertTrue(h.current!!.awaitingOppPositionSwitch)
        assertEquals(Feedback.CONFIRM, h.answerPrompt(1)) // OPP keep
        val s = h.current!!
        assertFalse(s.isAwaitingAnswer)
        assertEquals(size, h.size)
        assertEquals(listOf(Player.A2, Player.B2, Player.A1, Player.B1), s.config.serveOrder)
    }

    @Test fun `an answer outside the prompt's choices does nothing`() {
        val h = started(doublesConfig())
        h.scoreTimes(Team.YOU, 4)
        val before = h.current
        assertEquals(Feedback.NONE, h.answerPrompt(2))
        assertEquals(Feedback.NONE, h.answerPrompt(-1))
        assertSame(before, h.current)
        h.answerPrompt(0)
        h.scoreTimes(Team.YOU, 20)
        assertEquals(Feedback.NONE, h.answerPrompt(1), "the side change has one choice")
        assertTrue(h.current!!.awaitingCourtSideChange)
    }

    @Test fun `answerPrompt without a prompt does nothing`() {
        val h = started()
        assertEquals(Feedback.NONE, h.answerPrompt(0))
        assertEquals(1, h.size)
    }

    // ── End, summary and reopen ──────────────────────────────────────────────

    @Test fun `end moves to the summary and blocks undo and points`() {
        val h = started()
        h.score(Team.YOU)
        h.end(5_000)
        assertEquals(AppScreen.SUMMARY, h.screen)
        assertEquals(5_000, h.current!!.match.endedAt)
        assertFalse(h.canUndo)
        assertEquals(Feedback.NONE, h.undo())
        assertEquals(Feedback.NONE, h.score(Team.YOU))
    }

    @Test fun `end keeps the first end time and does not count as a change`() {
        val h = started()
        h.end(5_000)
        val revision = h.revision
        h.end(9_000)
        assertEquals(5_000, h.current!!.match.endedAt)
        assertEquals(revision, h.revision)
    }

    @Test fun `a won match waits for the summary until end is called`() {
        val h = won()
        assertTrue(h.isAwaitingSummary)
        assertEquals(AppScreen.MATCH, h.screen)
        h.end(9_000)
        assertFalse(h.isAwaitingSummary)
    }

    @Test fun `undo before the summary opens cancels the win`() {
        val h = won()
        assertEquals(Feedback.UNDO, h.undo())
        assertFalse(h.current!!.isMatchOver)
        assertFalse(h.isAwaitingSummary)
    }

    @Test fun `reopen after a win undoes the winning point`() {
        val h = won()
        h.end(9_000)
        h.reopen()
        assertEquals(AppScreen.MATCH, h.screen)
        assertFalse(h.current!!.isMatchOver)
        assertNull(h.current!!.match.endedAt)
        assertEquals(5, h.current!!.set.youGames)
    }

    @Test fun `reopen after stopping early resumes the same score`() {
        val h = started()
        h.scoreTimes(Team.YOU, 2)
        val size = h.size
        h.end(9_000)
        h.reopen()
        assertEquals(AppScreen.MATCH, h.screen)
        assertEquals(size, h.size)
        assertEquals(2, h.current!!.game.youPoints)
    }

    @Test fun `reopen during the match does nothing`() {
        val h = started()
        h.score(Team.YOU)
        val revision = h.revision
        h.reopen()
        assertEquals(2, h.size)
        assertEquals(revision, h.revision)
    }

    // ── Revision ─────────────────────────────────────────────────────────────

    @Test fun `every change raises the revision and a no-op does not`() {
        val h = MatchHistory()
        val revisions = mutableListOf(h.revision)
        fun step(action: MatchHistory.() -> Unit) {
            h.action()
            revisions += h.revision
        }
        step { start(singlesConfig(), 0) }
        step { score(Team.YOU) }
        step { undo() }
        step { end(1) }
        step { reopen() }
        step { clear() }
        assertEquals(revisions.sorted().distinct(), revisions, "strictly increasing: $revisions")

        val idle = h.revision
        h.clear()
        h.undo()
        h.score(Team.YOU)
        assertEquals(idle, h.revision)
    }

    @Test fun `undo raises the revision although the score goes back`() {
        val h = started()
        h.score(Team.YOU)
        val afterPoint = h.revision
        h.undo()
        assertTrue(h.revision > afterPoint)
    }

    // ── Epoch ────────────────────────────────────────────────────────────────

    @Test fun `every fresh history draws its own epoch`() {
        val epochs = List(5) { MatchHistory().epoch }
        assertEquals(epochs.size, epochs.distinct().size, "$epochs")
    }

    @Test fun `the epoch stays through every change and a new match`() {
        val h = MatchHistory()
        val epoch = h.epoch
        h.start(singlesConfig(), 0)
        h.score(Team.YOU)
        h.undo()
        h.end(1)
        h.reopen()
        h.clear()
        h.start(singlesConfig(), 2)
        assertEquals(epoch, h.epoch)
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    @Test fun `encode and decode round-trips the whole stack with its epoch and revision`() {
        val h = started(doublesConfig(RuleMode.STAR))
        h.scoreTimes(Team.YOU, 4)
        h.answerPrompt(0)
        h.scoreTimes(Team.OPP, 3)
        val restored = MatchHistory.decode(h.encode())!!
        assertEquals(h.size, restored.size)
        assertEquals(h.current, restored.current)
        assertEquals(h.epoch, restored.epoch)
        assertEquals(h.revision, restored.revision)
        restored.undo()
        h.undo()
        assertEquals(h.current, restored.current)
    }

    @Test fun `decode restores the summary screen`() {
        val h = started()
        h.end(5_000)
        assertEquals(AppScreen.SUMMARY, MatchHistory.decode(h.encode())!!.screen)
    }

    @Test fun `a cleared history restores as setup and keeps its epoch and revision`() {
        val h = started().apply {
            score(Team.YOU)
            clear()
        }
        val restored = MatchHistory.decode(h.encode())!!
        assertEquals(AppScreen.SETUP, restored.screen)
        assertEquals(h.epoch, restored.epoch)
        assertEquals(h.revision, restored.revision)
    }

    @Test fun `decode rejects garbage`() {
        assertNull(MatchHistory.decode("not json"))
        assertNull(MatchHistory.decode("{}"))
    }

    @Test fun `decode rejects another format version`() {
        val data = started().encode()
        assertTrue("\"version\":3" in data)
        assertNotNull(MatchHistory.decode(data))
        assertNull(MatchHistory.decode(data.replace("\"version\":3", "\"version\":2")))
    }

    @Test fun `decode rejects a history without an epoch`() {
        val h = started()
        val data = h.encode()
        assertTrue("\"epoch\":${h.epoch}," in data)
        assertNull(MatchHistory.decode(data.replace("\"epoch\":${h.epoch},", "")))
    }
}
