package ch.wastedpotential.padel.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.right
import androidx.compose.ui.test.up
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.link.MemoryChannel
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.match.Buzz
import ch.wastedpotential.padel.match.Feedback
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.presentation.Haptics
import ch.wastedpotential.padel.sync.Scoreboard
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class) // accessibility actions, the virtual clock
class MatchScreenTest {

    @get:Rule val rule = createComposeRule()

    private val scheduler = TestCoroutineScheduler()
    private fun showMatch(config: Config = singlesConfig(), haptics: Haptics = Haptics { _, _ -> }) =
        rule.showApp(testViewModel(config, scheduler = scheduler), haptics)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_bothScoresAreZero() {
        showMatch()
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
        rule.onNodeWithTag(UiTags.SCORE_OPP).assertTextEquals("0")
    }

    @Test
    fun initialState_teamNamesUndoAndServeStripeAreShown() {
        showMatch()
        rule.onNodeWithText(UiText.YOU).assertIsDisplayed()
        rule.onNodeWithText(UiText.OPPONENT).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.UNDO).assertIsDisplayed().assertContentDescriptionEquals(UiText.UNDO)
        rule.onNodeWithTag(UiTags.SERVE_STRIPE).assertIsDisplayed()
    }

    @Test
    fun namesFromThePhone_replaceTheLabels() {
        val phone = MemoryChannel()
        rule.showApp(testViewModel(singlesConfig(), phone))
        rule.runOnIdle { phone.receive(SyncKeys.NAMES_PATH, TeamNames("Ana & Bo", "Cy & Di").encode()) }
        rule.onNodeWithText("Ana & Bo").assertIsDisplayed()
        rule.onNodeWithText("Cy & Di").assertIsDisplayed()
        rule.onNodeWithTag(UiTags.TAP_YOU).assertContentDescriptionEquals(UiText.pointFor("Ana & Bo"))
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    @Test
    fun tappingYouZone_incrementsYourScore() {
        showMatch()
        rule.tap(UiTags.TAP_YOU)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("15")
        rule.onNodeWithTag(UiTags.SCORE_OPP).assertTextEquals("0")
    }

    @Test
    fun tappingOppZone_incrementsOpponentScore() {
        showMatch()
        rule.tap(UiTags.TAP_OPP)
        rule.onNodeWithTag(UiTags.SCORE_OPP).assertTextEquals("15")
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
    }

    @Test
    fun scoringSequence_followsStandardPointProgression() {
        showMatch()
        rule.tap(UiTags.TAP_YOU, 3)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("40")
    }

    @Test
    fun aPoint_vibratesItsPattern() {
        val played = mutableListOf<List<Long>>()
        showMatch(haptics = Haptics { timings, _ -> played += timings.toList() })
        rule.tap(UiTags.TAP_OPP)
        rule.tap(UiTags.UNDO)
        assertEquals(listOf(Feedback.POINT_OPP, Feedback.UNDO).map { Buzz.of(it)!!.timings.toList() }, played)
    }

    @Test
    fun everyPoint_isMirroredToThePhone() {
        val phone = MemoryChannel()
        rule.showApp(testViewModel(singlesConfig(), phone))
        rule.tap(UiTags.TAP_YOU)
        val board = Scoreboard.decode(phone.lastSent(SyncKeys.SCOREBOARD_PATH)!!)!!
        assertEquals("15", board.match!!.youScore)
    }

    @Test
    fun tieBreak_showsThePill() {
        showMatch()
        repeat(5) {
            rule.tap(UiTags.TAP_YOU, 4)
            rule.tap(UiTags.TAP_OPP, 4)
        }
        rule.tap(UiTags.TAP_YOU, 4)
        rule.tap(UiTags.TAP_OPP, 4)
        rule.onNodeWithText(UiText.TIE_BREAK).assertIsDisplayed()
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_revertsThePoints() {
        showMatch()
        rule.tap(UiTags.TAP_YOU, 2)
        rule.tap(UiTags.UNDO)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("15")
        rule.tap(UiTags.UNDO)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
    }

    @Test
    fun undo_atStart_doesNothing() {
        val vm = showMatch()
        rule.tap(UiTags.UNDO)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
    }

    // ── Status pill ───────────────────────────────────────────────────────────

    @Test
    fun matchPoint_pillIsShown() {
        showMatch()
        rule.tap(UiTags.TAP_YOU, 24)
        rule.tap(UiTags.COURT_SIDE_ACK)
        rule.tap(UiTags.TAP_YOU, 23)
        rule.onNodeWithText(UiText.MATCH_POINT).assertIsDisplayed()
    }

    @Test
    fun goldenPoint_pillIsShown_andTheNextPointDecides() {
        showMatch(singlesConfig(ruleMode = RuleMode.GOLDEN))
        rule.tap(UiTags.TAP_YOU, 3)
        rule.tap(UiTags.TAP_OPP, 3)
        rule.onNodeWithText(UiText.GOLDEN_POINT).assertIsDisplayed()
        rule.tap(UiTags.TAP_YOU)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
        rule.onNodeWithTag(UiTags.SCORE_OPP).assertTextEquals("0")
        rule.onNodeWithText(UiText.GOLDEN_POINT).assertDoesNotExist()
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    @Test
    fun doublesGame2_showsServePick() {
        showMatch(doublesConfig())
        rule.tap(UiTags.TAP_YOU, 4)
        rule.onNodeWithText(UiText.WHO_SERVES).assertIsDisplayed()
        rule.onNodeWithText(UiText.LEFT).assertIsDisplayed()
        rule.onNodeWithText(UiText.RIGHT).assertIsDisplayed()
        rule.tap(UiTags.PICK_RIGHT)
        rule.onNodeWithText(UiText.WHO_SERVES).assertDoesNotExist()
    }

    @Test
    fun afterSet1_showsChangeSides_untilTapped() {
        showMatch()
        rule.tap(UiTags.TAP_YOU, 24)
        rule.onNodeWithText(UiText.CHANGE_SIDES).assertIsDisplayed()
        rule.onNodeWithText(UiText.TAP_TO_CONTINUE).assertIsDisplayed()
        rule.tap(UiTags.COURT_SIDE_ACK)
        rule.onNodeWithText(UiText.CHANGE_SIDES).assertDoesNotExist()
    }

    @Test
    fun singlesAfterSet1_doesNotAskForPositionSwitch() {
        showMatch()
        rule.tap(UiTags.TAP_YOU, 24)
        rule.tap(UiTags.COURT_SIDE_ACK)
        rule.onNodeWithText(UiText.SWITCH_SIDES).assertDoesNotExist()
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
    }

    @Test
    fun doublesAfterSet1_asksBothTeamsToSwitch_thenContinues() {
        showMatch(doublesConfig())
        playDoublesSetYou()
        rule.tap(UiTags.COURT_SIDE_ACK)
        rule.onNodeWithText(UiText.SWITCH_SIDES).assertIsDisplayed()
        rule.onNodeWithText(UiText.YOUR_TEAM).assertIsDisplayed()
        rule.tap(UiTags.SWITCH_YES)
        rule.onNodeWithText(UiText.OPP_TEAM).assertIsDisplayed()
        rule.tap(UiTags.SWITCH_NO)
        rule.onNodeWithText(UiText.SWITCH_SIDES).assertDoesNotExist()
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("0")
    }

    /** A 6-0 doubles set for you; the serve pick after game 1 is answered right. */
    private fun playDoublesSetYou() {
        rule.tap(UiTags.TAP_YOU, 4)
        rule.tap(UiTags.PICK_RIGHT)
        rule.tap(UiTags.TAP_YOU, 20)
    }

    // ── End match ─────────────────────────────────────────────────────────────

    @Test
    fun holdingUndo_thenTapping_endsTheMatch() {
        val vm = showMatch()
        rule.tap(UiTags.TAP_YOU)
        rule.onNodeWithTag(UiTags.UNDO).performTouchInput { longClick(durationMillis = UiTimings.END_MATCH_HOLD_MS + HOLD_MARGIN_MS) }
        rule.onNodeWithContentDescription(UiText.END_MATCH).assertIsDisplayed()
        rule.tap(UiTags.UNDO)
        rule.onNodeWithText(UiText.MATCH_ENDED).assertIsDisplayed()
        assertEquals(AppScreen.SUMMARY, vm.state!!.screen)
        assertEquals(1, vm.state!!.summary!!.pointsYou)
    }

    @Test
    fun winningTheMatch_opensTheSummaryAfterAPause() {
        val vm = showMatch()
        rule.tap(UiTags.TAP_YOU, 24)
        rule.tap(UiTags.COURT_SIDE_ACK)
        rule.tap(UiTags.TAP_YOU, 24)
        rule.runOnIdle {
            scheduler.advanceTimeBy(UiTimings.AUTO_SUMMARY_DELAY_MS - 1)
            scheduler.runCurrent()
        }
        assertEquals("the final score stays up first", AppScreen.MATCH, vm.state!!.screen)
        rule.runOnIdle {
            scheduler.advanceTimeBy(1)
            scheduler.runCurrent()
        }
        rule.onNodeWithText(UiText.YOU_WIN).assertIsDisplayed()
    }

    // ── TalkBack ──────────────────────────────────────────────────────────────

    @Test
    fun talkBack_zonesAreLabelledAndScoreOnClick() {
        showMatch()
        rule.onNodeWithTag(UiTags.TAP_OPP).assertContentDescriptionEquals(UiText.pointFor(UiText.OPPONENT))
        rule.onNodeWithTag(UiTags.TAP_YOU).assertContentDescriptionEquals(UiText.pointFor(UiText.YOU))
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("15")
    }

    @Test
    fun talkBack_scoresForEitherTeamFromEitherZone() {
        showMatch()
        rule.onNodeWithTag(UiTags.TAP_YOU).performCustomAccessibilityActionWithLabel(UiText.pointFor(UiText.OPPONENT))
        rule.onNodeWithTag(UiTags.TAP_OPP).performCustomAccessibilityActionWithLabel(UiText.pointFor(UiText.YOU))
        rule.onNodeWithTag(UiTags.SCORE_OPP).assertTextEquals("15")
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("15")
    }

    @Test
    fun talkBack_undoesAndEndsTheMatchWithoutAHold() {
        val vm = showMatch()
        rule.tap(UiTags.TAP_YOU, 2)
        rule.onNodeWithTag(UiTags.TAP_YOU).performCustomAccessibilityActionWithLabel(UiText.UNDO)
        rule.onNodeWithTag(UiTags.SCORE_YOU).assertTextEquals("15")
        rule.onNodeWithTag(UiTags.TAP_YOU).performCustomAccessibilityActionWithLabel(UiText.END_MATCH)
        rule.onNodeWithText(UiText.MATCH_ENDED).assertIsDisplayed()
        assertEquals(1, vm.state!!.summary!!.pointsYou)
    }

    private companion object {
        /** Past the hold by a few frames, so the ring has surely filled. */
        const val HOLD_MARGIN_MS = 300L
    }
}
