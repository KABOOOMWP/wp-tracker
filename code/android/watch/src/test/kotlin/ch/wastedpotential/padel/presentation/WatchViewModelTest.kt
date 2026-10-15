package ch.wastedpotential.padel.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.link.MemoryChannel
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.match.Feedback
import ch.wastedpotential.padel.match.MatchSetup
import ch.wastedpotential.padel.match.SessionState
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.sync.Scoreboard
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WatchViewModelTest {

    private val singles = MatchSetup.config(PlayMode.SINGLES, 3, RuleMode.STANDARD, Team.YOU, false)
    private val doubles = MatchSetup.config(PlayMode.DOUBLES, 3, RuleMode.STANDARD, Team.YOU, false)
    private val matchStore = MemoryTextStore()
    private val namesStore = MemoryTextStore()
    private val phone = MemoryChannel()
    private var now = 1_000L

    // Loading and the auto-summary timer run here: at once, on this dispatcher's virtual clock
    private val dispatcher = UnconfinedTestDispatcher()

    private fun vm(link: MemoryChannel? = phone, openMatch: suspend () -> TextStore = { matchStore }) =
        WatchViewModel(openMatch, { namesStore }, link, { now }, CoroutineScope(dispatcher))

    private val WatchViewModel.shown: SessionState get() = assertNotNull(state, "the stores are open")

    private fun lastBoard() = Scoreboard.decode(phone.lastSent(SyncKeys.SCOREBOARD_PATH)!!)!!

    /** A 6-0 6-0 singles win, the side change answered. */
    private fun WatchViewModel.win() {
        repeat(24) { score(Team.YOU) }
        answerPrompt(0)
        repeat(24) { score(Team.YOU) }
    }

    // ── Opening ───────────────────────────────────────────────────────────────

    @Test fun `nothing is drawn or played until the saved match is read`() {
        vm().apply {
            start(singles)
            score(Team.YOU)
        }
        val opening = CompletableDeferred<TextStore>()
        val vm = vm(openMatch = { opening.await() })
        assertNull(vm.state, "no setup screen flashes before the saved match")
        assertEquals(Feedback.NONE, vm.score(Team.YOU))
        assertFalse(vm.back())

        opening.complete(matchStore)
        assertEquals(AppScreen.MATCH, vm.shown.screen)
        assertEquals("15", vm.shown.display!!.youScore)
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    @Test fun `starts on setup without a saved match`() {
        val vm = vm()
        assertEquals(AppScreen.SETUP, vm.shown.screen)
        assertNull(vm.shown.display)
    }

    @Test fun `start shows the match and saves it`() {
        vm().apply { start(singles) }
        val restored = vm()
        assertEquals(AppScreen.MATCH, restored.shown.screen)
        assertEquals("0", restored.shown.display!!.youScore)
    }

    @Test fun `score updates the state and returns its feedback`() {
        val vm = vm().apply { start(singles) }
        assertEquals(Feedback.POINT_YOU, vm.score(Team.YOU))
        assertEquals(Feedback.POINT_OPP, vm.score(Team.OPP))
        assertEquals("15", vm.shown.display!!.youScore)
        assertEquals("15", vm.shown.display!!.oppScore)
        assertTrue(vm.shown.canUndo)
    }

    @Test fun `undo reports whether anything was undone`() {
        val vm = vm().apply { start(singles) }
        assertEquals(Feedback.NONE, vm.undo())
        vm.score(Team.OPP)
        assertEquals(Feedback.UNDO, vm.undo())
        assertEquals("0", vm.shown.display!!.oppScore)
        assertFalse(vm.shown.canUndo)
    }

    @Test fun `a saved match is restored on launch, undo included`() {
        vm().apply {
            start(singles)
            score(Team.YOU)
            score(Team.YOU)
        }
        val restored = vm()
        assertEquals("30", restored.shown.display!!.youScore)
        assertEquals(Feedback.UNDO, restored.undo())
        assertEquals("15", restored.shown.display!!.youScore)
    }

    @Test fun `prompts are answered through the view model`() {
        val vm = vm().apply { start(doubles) }
        repeat(4) { vm.score(Team.YOU) }
        assertNotNull(vm.shown.display!!.prompt)
        assertEquals(Feedback.NONE, vm.score(Team.YOU), "no point while the serve pick is open")
        assertEquals(Feedback.CONFIRM, vm.answerPrompt(0))
        assertNull(vm.shown.display!!.prompt)
    }

    @Test fun `winning waits for the summary until end`() {
        val vm = vm().apply { start(singles) }
        repeat(24) { vm.score(Team.YOU) }
        assertEquals(Feedback.SIDE_CHANGE, vm.answerPrompt(0))
        repeat(24) { vm.score(Team.YOU) }
        assertTrue(vm.shown.isAwaitingSummary)
        now = 61_000L
        vm.end()
        assertFalse(vm.shown.isAwaitingSummary)
        assertEquals(AppScreen.SUMMARY, vm.shown.screen)
        assertEquals(UiText.YOU_WIN, vm.shown.summary!!.resultLabel)
    }

    @Test fun `a win opens the summary after the delay`() = runTest(dispatcher) {
        val vm = vm().apply {
            start(singles)
            win()
        }
        advanceTimeBy(UiTimings.AUTO_SUMMARY_DELAY_MS - 1)
        runCurrent()
        assertEquals(AppScreen.MATCH, vm.shown.screen, "the final score stays up first")
        advanceTimeBy(1)
        runCurrent()
        assertEquals(AppScreen.SUMMARY, vm.shown.screen)
        assertEquals(UiText.YOU_WIN, vm.shown.summary!!.resultLabel)
    }

    @Test fun `an undo in the delay cancels the summary`() = runTest(dispatcher) {
        val vm = vm().apply {
            start(singles)
            win()
        }
        advanceTimeBy(UiTimings.AUTO_SUMMARY_DELAY_MS / 2)
        assertEquals(Feedback.UNDO, vm.undo())
        advanceUntilIdle()
        assertEquals(AppScreen.MATCH, vm.shown.screen)
        assertFalse(vm.shown.isAwaitingSummary)
    }

    @Test fun `a won match restored before its summary opens it after the delay`() = runTest(dispatcher) {
        vm().apply {
            start(singles)
            win()
        }
        val restored = vm()
        assertTrue(restored.shown.isAwaitingSummary)
        advanceUntilIdle()
        assertEquals(AppScreen.SUMMARY, restored.shown.screen)
    }

    @Test fun `a setup tap during a match does nothing`() {
        val vm = vm().apply { start(singles) }
        assertEquals(Feedback.NONE, vm.chooseSetup(0))
        assertEquals(AppScreen.MATCH, vm.shown.screen)
    }

    @Test fun `reopen returns to the match and new match starts the next setup, also after a relaunch`() {
        val vm = vm().apply {
            start(singles)
            score(Team.YOU)
            end()
        }
        vm.reopen()
        assertEquals(AppScreen.MATCH, vm.shown.screen)
        assertEquals("15", vm.shown.display!!.youScore)
        vm.end()
        vm.newMatch()
        assertEquals(AppScreen.SETUP, vm.shown.screen)
        assertEquals(UiText.PLAY_MODE, vm.shown.setup!!.pills.single())
        assertEquals(AppScreen.SETUP, vm().shown.screen)
    }

    // ── Setup and back ────────────────────────────────────────────────────────

    @Test fun `the setup answers start a singles match`() {
        val vm = vm()
        listOf(0, 1, 2, 0).forEach { assertEquals(Feedback.CONFIRM, vm.chooseSetup(it)) } // 1 vs 1, best of 5, star point, opponent serves
        assertEquals(AppScreen.MATCH, vm.shown.screen)
        assertEquals(Team.OPP, vm.shown.display!!.servingTeam)
        assertNull(vm.shown.setup)
        assertEquals(AppScreen.MATCH, vm().shown.screen, "the started match is saved")
    }

    @Test fun `doubles setup ends with the server picker`() {
        val vm = vm()
        listOf(1, 0, 0, 1).forEach(vm::chooseSetup)
        assertNotNull(vm.shown.setup!!.picker)
        vm.chooseSetup(0)
        assertEquals(AppScreen.MATCH, vm.shown.screen)
    }

    @Test fun `an answer outside the screen's choices does nothing`() {
        val vm = vm()
        val first = vm.shown.setup
        assertEquals(Feedback.NONE, vm.chooseSetup(5))
        assertEquals(first, vm.shown.setup)
    }

    @Test fun `back steps through the setup, is swallowed in a match and free on the summary`() {
        val vm = vm()
        val first = vm.shown.setup
        assertFalse(vm.shown.handlesBack)
        vm.chooseSetup(0)
        assertTrue(vm.shown.handlesBack)
        assertTrue(vm.back())
        assertEquals(first, vm.shown.setup)
        vm.start(singles)
        vm.score(Team.YOU)
        assertTrue(vm.shown.handlesBack)
        assertTrue(vm.back())
        assertEquals(AppScreen.MATCH, vm.shown.screen)
        assertEquals("15", vm.shown.display!!.youScore, "back neither ends nor undoes")
        vm.end()
        assertFalse(vm.shown.handlesBack)
        assertFalse(vm.back())
    }

    // ── Phone link ────────────────────────────────────────────────────────────

    @Test fun `the current state is sent to the phone on launch`() {
        vm()
        assertEquals(AppScreen.SETUP, lastBoard().screen)
    }

    @Test fun `every change is mirrored to the phone with a growing revision`() {
        val vm = vm().apply { start(singles) }
        val started = lastBoard().revision
        vm.score(Team.YOU)
        val point = lastBoard()
        assertEquals("15", point.match!!.youScore)
        assertTrue(point.revision > started)
        vm.undo()
        assertTrue(lastBoard().revision > point.revision, "an undo is a newer board too")
        vm.end()
        assertEquals(AppScreen.SUMMARY, lastBoard().screen)
    }

    @Test fun `a relaunch keeps the epoch and counts on, so the phone takes its boards`() {
        vm().apply {
            start(singles)
            score(Team.YOU)
        }
        val before = lastBoard()
        vm().score(Team.YOU)
        assertEquals(before.epoch, lastBoard().epoch)
        assertTrue(lastBoard().revision > before.revision)
    }

    @Test fun `a reinstall starts a new epoch, so the phone takes its lower revisions`() {
        vm().apply {
            start(singles)
            repeat(10) { score(Team.YOU) }
        }
        val before = lastBoard()
        vm(openMatch = { MemoryTextStore() }).start(singles)
        assertTrue(lastBoard().epoch != before.epoch)
        assertTrue(lastBoard().revision < before.revision)
    }

    @Test fun `names from the phone are shown and kept`() {
        val vm = vm()
        phone.receive(SyncKeys.NAMES_PATH, TeamNames("Ana", "Bo").encode())
        assertEquals(TeamNames("Ana", "Bo"), vm.names)
        assertEquals(TeamNames("Ana", "Bo"), vm(MemoryChannel()).names)
    }

    @Test fun `unreadable names are ignored`() {
        val vm = vm()
        phone.receive(SyncKeys.NAMES_PATH, "garbage")
        assertEquals(TeamNames.DEFAULT, vm.names)
    }

    @Test fun `names the phone sent before the watch app started are picked up`() {
        val (watchSide, phoneSide) = MemoryChannel.pair()
        phoneSide.send(SyncKeys.NAMES_PATH, TeamNames("Ana", "Bo").encode())
        assertEquals(TeamNames("Ana", "Bo"), vm(watchSide).names)
    }

    @Test fun `clearing the view model closes the phone link`() {
        val store = ViewModelStore()
        ViewModelProvider.create(store, viewModelFactory { initializer { vm() } })[WatchViewModel::class]
        assertFalse(phone.isClosed)
        store.clear()
        assertTrue(phone.isClosed)
    }

    @Test fun `works without a phone`() {
        val vm = vm(link = null).apply { start(singles) }
        assertEquals(Feedback.POINT_YOU, vm.score(Team.YOU))
        assertTrue(phone.sent.isEmpty())
    }
}
