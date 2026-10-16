package ch.wastedpotential.padel.ui

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import ch.wastedpotential.padel.display.PromptKind
import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.link.MemoryChannel
import ch.wastedpotential.padel.match.MatchSetup
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.presentation.Haptics
import ch.wastedpotential.padel.presentation.WatchApp
import ch.wastedpotential.padel.presentation.WatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** A view model on in-memory stores; it opens at once, and its timers (the auto summary) run on [scheduler]. */
@OptIn(ExperimentalCoroutinesApi::class)
fun testViewModel(config: Config? = null, phone: MemoryChannel? = null, scheduler: TestCoroutineScheduler = TestCoroutineScheduler()) =
    WatchViewModel({ MemoryTextStore() }, { MemoryTextStore() }, phone, { 0L }, CoroutineScope(UnconfinedTestDispatcher(scheduler)))
        .apply { config?.let(::start) }

/** Shows the whole watch app on [vm], as MainActivity does; [haptics] plays nothing unless a test listens. */
fun ComposeContentTestRule.showApp(
    vm: WatchViewModel = testViewModel(),
    haptics: Haptics = Haptics { _, _ -> },
    undoHoldMs: Long = UiTimings.END_MATCH_HOLD_MS,
): WatchViewModel {
    setContent { WatchApp(vm, haptics, undoHoldMs) }
    waitForIdle()
    return vm
}

fun ComposeContentTestRule.tap(tag: String, times: Int = 1) = repeat(times) { onNodeWithTag(tag).performTouchInput { click() } }

/** A part of the match board, which is one element for TalkBack, so it is found in the unmerged tree. */
fun ComposeContentTestRule.boardPart(tag: String) = onNodeWithTag(tag, useUnmergedTree = true)

fun singlesConfig(bestOf: Int = 3, ruleMode: RuleMode = RuleMode.STANDARD, youServe: Boolean = true) =
    MatchSetup.config(PlayMode.SINGLES, bestOf, ruleMode, if (youServe) Team.YOU else Team.OPP, false)

fun doublesConfig(bestOf: Int = 3, ruleMode: RuleMode = RuleMode.STANDARD, youServe: Boolean = true) =
    MatchSetup.config(PlayMode.DOUBLES, bestOf, ruleMode, if (youServe) Team.YOU else Team.OPP, false)

/** A view model on the summary of a finished 6-0 6-0 match won by [winner]; prompts get their last answer. */
fun finishedMatch(config: Config = singlesConfig(), winner: Team = Team.YOU) = testViewModel(config).apply {
    repeat(2) {
        repeat(24) { score(winner) }
        while (true) {
            val prompt = state?.display?.prompt ?: break
            answerPrompt(if (prompt.kind == PromptKind.TAP) 0 else 1)
        }
    }
    end()
}
