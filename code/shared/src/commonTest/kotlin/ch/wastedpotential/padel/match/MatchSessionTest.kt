package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.setup.SetupState
import ch.wastedpotential.padel.sync.PhoneBoard
import ch.wastedpotential.padel.sync.Scoreboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchSessionTest {

    /** A store that counts its saves. */
    private class CountingStore(private val inner: MemoryTextStore = MemoryTextStore()) : TextStore by inner {
        var saves = 0
        override fun save(data: String) {
            saves++
            inner.save(data)
        }
    }

    private var now = 1_000L
    private val sent = mutableListOf<Scoreboard>()
    private fun session(store: TextStore = MemoryTextStore()) = MatchSession(store, { sent += it }, { now })

    /** Wins a 6-0 6-0 singles match; the summary is not shown yet. */
    private fun MatchSession.winMatch() {
        repeat(24) { score(Team.YOU) }
        answerPrompt(0)
        repeat(24) { score(Team.YOU) }
    }

    @Test fun `starts on setup`() {
        val s = session()
        assertEquals(AppScreen.SETUP, s.state.screen)
        assertNull(s.state.display)
    }

    @Test fun `start shows the match saves it and tells the phone`() {
        val store = MemoryTextStore()
        val s = session(store)
        var changes = 0
        s.onChange = { changes++ }
        s.start(singlesConfig())
        assertEquals(AppScreen.MATCH, s.state.screen)
        assertEquals("0", s.state.display!!.youScore)
        assertEquals(AppScreen.MATCH, MatchHistory.decode(store.load()!!)!!.screen)
        assertEquals(AppScreen.MATCH, sent.last().screen)
        assertEquals(1, changes)
    }

    @Test fun `score returns the feedback and updates everything`() {
        val s = session().apply { start(singlesConfig()) }
        assertEquals(Feedback.POINT_YOU, s.score(Team.YOU))
        assertEquals("15", s.state.display!!.youScore)
        assertTrue(s.state.canUndo)
        assertEquals("15", sent.last().match!!.youScore)
    }

    @Test fun `a no-op neither saves nor notifies`() {
        val store = CountingStore()
        val s = session(store)
        var changes = 0
        s.onChange = { changes++ }
        assertEquals(Feedback.NONE, s.score(Team.YOU))
        assertEquals(Feedback.NONE, s.undo())
        s.end()
        s.reopen()
        assertEquals(0, store.saves)
        assertEquals(0, changes)
        assertTrue(sent.isEmpty())
    }

    @Test fun `end and reopen save only when they change the match`() {
        val store = CountingStore()
        val s = session(store).apply {
            start(singlesConfig())
            score(Team.YOU)
        }
        var saves = store.saves
        s.reopen()
        assertEquals(saves, store.saves, "reopen during a match")
        s.end()
        assertEquals(++saves, store.saves)
        s.end()
        assertEquals(saves, store.saves, "end on the summary")
        s.reopen()
        assertEquals(++saves, store.saves)
    }

    @Test fun `undo and prompts go through the session`() {
        val s = session().apply { start(singlesConfig()) }
        s.score(Team.OPP)
        assertEquals(Feedback.UNDO, s.undo())
        repeat(24) { s.score(Team.YOU) }
        assertNotNull(s.state.display!!.prompt)
        assertEquals(Feedback.SIDE_CHANGE, s.answerPrompt(0))
        assertNull(s.state.display!!.prompt)
    }

    @Test fun `a saved match is restored`() {
        val store = MemoryTextStore()
        session(store).apply {
            start(singlesConfig())
            score(Team.YOU)
            score(Team.YOU)
        }
        val restored = session(store)
        assertEquals("30", restored.state.display!!.youScore)
        assertEquals(Feedback.UNDO, restored.undo())
    }

    @Test fun `win summary reopen and new match`() {
        val store = MemoryTextStore()
        val s = session(store).apply { start(singlesConfig()) }
        s.winMatch()
        assertTrue(s.state.isAwaitingSummary)

        now = 61_000L
        s.end()
        assertEquals(AppScreen.SUMMARY, s.state.screen)
        assertEquals("00:01", s.state.summary!!.duration)
        assertEquals(AppScreen.SUMMARY, sent.last().screen)

        s.reopen()
        assertEquals(AppScreen.MATCH, s.state.screen)

        s.end()
        s.newMatch()
        assertEquals(AppScreen.SETUP, s.state.screen)
        assertEquals(AppScreen.SETUP, sent.last().screen)
        assertEquals(AppScreen.SETUP, session(store).state.screen, "a relaunch stays on setup")
    }

    // ── Phone ordering ───────────────────────────────────────────────────────

    @Test fun `the phone follows every change while the watch clock jumps back`() {
        val phone = PhoneBoard()
        val s = MatchSession(MemoryTextStore(), { phone.receive(it.encode()) }, { now })
        s.start(singlesConfig())
        s.score(Team.YOU)
        now -= 3_600_000L
        s.score(Team.YOU)
        assertEquals("30", phone.match!!.youScore)
        now -= 3_600_000L
        s.undo()
        assertEquals("15", phone.match!!.youScore)
    }

    @Test fun `revisions keep growing and the epoch stays across a relaunch and a new match`() {
        val store = MemoryTextStore()
        session(store).apply {
            start(singlesConfig())
            score(Team.YOU)
            end()
        }
        val beforeRelaunch = sent.last()
        session(store).apply { newMatch() }
        val afterNewMatch = sent.last()
        assertTrue(afterNewMatch.revision > beforeRelaunch.revision)
        session(store).apply { start(singlesConfig()) }
        assertTrue(sent.last().revision > afterNewMatch.revision)
        assertEquals(setOf(beforeRelaunch.epoch), sent.map { it.epoch }.toSet())
    }

    @Test fun `a reinstalled watch is followed from its first board although its revisions start over`() {
        val phone = PhoneBoard()
        val sink = ScoreboardSink { phone.receive(it.encode()) }
        MatchSession(MemoryTextStore(), sink, { now }).apply {
            start(singlesConfig())
            repeat(10) { score(Team.YOU) }
        }
        val worn = phone.board!!
        val reinstalled = MatchSession(MemoryTextStore(), sink, { now }).apply { start(singlesConfig()) }
        assertEquals("0", phone.match!!.youScore)
        assertTrue(phone.board!!.revision < worn.revision)
        assertTrue(phone.board!!.epoch != worn.epoch)
        reinstalled.score(Team.OPP)
        assertEquals("15", phone.match!!.oppScore)
        assertEquals(Feedback.UNDO, reinstalled.undo())
        assertEquals("0", phone.match!!.oppScore, "undo is a newer board too")
    }

    @Test fun `announce resends the current board with the same revision`() {
        val s = session().apply { start(singlesConfig()) }
        val board = sent.last()
        sent.clear()
        s.announce()
        assertEquals(board, sent.single())
    }

    @Test fun `works without a phone`() {
        val s = MatchSession(MemoryTextStore(), null, { now })
        s.start(singlesConfig())
        assertEquals(Feedback.POINT_YOU, s.score(Team.YOU))
    }

    @Test fun `unreadable saved data starts fresh on setup`() {
        val store = MemoryTextStore("{not a match}")
        val s = session(store)
        assertEquals(AppScreen.SETUP, s.state.screen)
        s.start(singlesConfig())
        assertEquals(AppScreen.MATCH, session(store).state.screen, "the next save replaces the bad data")
    }

    @Test fun `without a clock the match runs on the wall clock`() {
        val s = MatchSession(MemoryTextStore(), null)
        s.start(singlesConfig())
        s.end()
        assertEquals("00:00", s.state.summary!!.duration)
    }

    // ── Setup and back ───────────────────────────────────────────────────────

    @Test fun `setup is drawn from the session and its answers confirm`() {
        val s = session()
        var changes = 0
        s.onChange = { changes++ }
        assertEquals(SetupState().screen, s.state.setup)
        assertEquals(Feedback.CONFIRM, s.chooseSetup(1))
        assertEquals(SetupState().choose(1)!!.screen, s.state.setup)
        assertEquals(1, changes)
        assertTrue(sent.isEmpty(), "setup steps are not sent to the phone")
    }

    @Test fun `a setup answer outside the choices does nothing`() {
        val s = session()
        var changes = 0
        s.onChange = { changes++ }
        assertEquals(Feedback.NONE, s.chooseSetup(2))
        assertEquals(Feedback.NONE, s.chooseSetup(-1))
        assertEquals(SetupState().screen, s.state.setup)
        assertEquals(0, changes)
    }

    @Test fun `the last setup answer starts the match`() {
        val store = MemoryTextStore()
        val s = session(store)
        listOf(1, 0, 0, 1).forEach { assertEquals(Feedback.CONFIRM, s.chooseSetup(it)) }
        assertEquals(AppScreen.SETUP, s.state.screen, "doubles still asks which player serves")
        s.chooseSetup(0)
        assertEquals(AppScreen.MATCH, s.state.screen)
        assertNull(s.state.setup)
        assertEquals(PlayMode.DOUBLES, MatchHistory.decode(store.load()!!)!!.current!!.config.playMode)
    }

    @Test fun `a new match starts setup from the first step`() {
        val s = session()
        listOf(0, 0, 0, 1).forEach { s.chooseSetup(it) }
        s.end()
        s.newMatch()
        assertEquals(SetupState().screen, s.state.setup)
    }

    @Test fun `a new match during setup returns to the first step`() {
        val s = session()
        s.chooseSetup(0)
        var changes = 0
        s.onChange = { changes++ }
        s.newMatch()
        assertEquals(SetupState().screen, s.state.setup)
        assertEquals(1, changes)
    }

    @Test fun `a setup tap during a match does nothing`() {
        val s = session().apply { start(singlesConfig()) }
        val before = s.state
        var changes = 0
        s.onChange = { changes++ }
        assertEquals(Feedback.NONE, s.chooseSetup(0))
        assertEquals(before, s.state)
        assertEquals(0, changes)
    }

    @Test fun `a setup tap on the summary does nothing`() {
        val s = session().apply {
            start(singlesConfig())
            end()
        }
        assertEquals(Feedback.NONE, s.chooseSetup(1))
        assertEquals(AppScreen.SUMMARY, s.state.screen)
    }

    @Test fun `back steps through setup and is swallowed in a match`() {
        val s = session()
        assertFalse(s.state.handlesBack)
        assertFalse(s.back(), "the first step lets the app close")
        s.chooseSetup(0)
        s.chooseSetup(1)
        assertTrue(s.state.handlesBack)
        assertTrue(s.back())
        assertEquals(SetupState().choose(0)!!.screen, s.state.setup)
        assertTrue(s.back())
        assertEquals(SetupState().screen, s.state.setup)

        s.start(singlesConfig())
        assertTrue(s.state.handlesBack)
        assertTrue(s.back())
        assertEquals(AppScreen.MATCH, s.state.screen)

        s.end()
        assertFalse(s.state.handlesBack)
        assertFalse(s.back())
        assertEquals(AppScreen.SUMMARY, s.state.screen)
    }

    @Test fun `a restored match does not handle setup`() {
        val store = MemoryTextStore()
        session(store).start(singlesConfig())
        val restored = session(store)
        assertNull(restored.state.setup)
        assertTrue(restored.state.handlesBack)
    }
}
