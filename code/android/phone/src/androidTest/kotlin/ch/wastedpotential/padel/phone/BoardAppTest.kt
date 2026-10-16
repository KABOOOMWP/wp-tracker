package ch.wastedpotential.padel.phone

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.link.MemoryChannel
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.sync.SampleBoards
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardAppTest {

    @get:Rule val rule = createComposeRule()

    private val watch = MemoryChannel()

    private fun show(sample: String? = null): BoardViewModel {
        val vm = BoardViewModel({ MemoryTextStore() }, watch, sample?.let { SampleBoards.make(it)!!.encode() })
        rule.setContent { BoardApp(vm) }
        rule.waitForIdle()
        return vm
    }

    @Test
    fun withoutAMatch_waitsAndShowsTheNames() {
        show()
        rule.onNodeWithText(UiText.WAITING).assertIsDisplayed()
        rule.onNodeWithText(UiText.START_ON_WATCH).assertIsDisplayed()
        rule.onNodeWithText(UiText.YOU).assertIsDisplayed()
        rule.onNodeWithText(UiText.VERSUS).assertIsDisplayed()
        rule.onNodeWithText(UiText.OPPONENT).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).assertIsDisplayed()
    }

    // The match board is one element for TalkBack, so its parts are found in the unmerged tree
    private fun boardPart(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true)

    @Test
    fun aMatch_showsTheWatchBoard() {
        show(SampleBoards.MATCH)
        boardPart(UiTags.SCORE_YOU).assertTextEquals("40")
        boardPart(UiTags.SCORE_OPP).assertTextEquals("15")
        rule.onNodeWithText(UiText.YOU).assertIsDisplayed()
        rule.onNodeWithText(UiText.OPPONENT).assertIsDisplayed()
        boardPart(UiTags.SERVE_STRIPE).assertExists()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).assertIsDisplayed()
        rule.onNodeWithText(UiText.WAITING).assertDoesNotExist()
    }

    @Test
    fun theBoard_readsAsOneElement() {
        show(SampleBoards.MATCH)
        val display = SampleBoards.make(SampleBoards.MATCH)!!.match!!
        rule.onNodeWithTag(UiTags.MATCH_BOARD).assertContentDescriptionEquals(display.spokenScore(TeamNames.DEFAULT))
    }

    @Test
    fun aScoreboardFromTheWatch_replacesTheWaitingScreen() {
        show()
        rule.runOnIdle { watch.receive(SyncKeys.SCOREBOARD_PATH, SampleBoards.make(SampleBoards.MATCH)!!.encode()) }
        boardPart(UiTags.SCORE_YOU).assertTextEquals("40")
    }

    @Test
    fun aSummary_showsResultsAndStats_withoutTheEditButton() {
        show(SampleBoards.SUMMARY)
        rule.onNodeWithText(UiText.MATCH_SUMMARY).assertIsDisplayed()
        rule.onNodeWithText(UiText.YOU_WIN).assertIsDisplayed()
        rule.onNodeWithText(TeamNames.DEFAULT.summaryName(Team.OPP)).assertIsDisplayed()
        rule.onNodeWithText(UiText.STAT_POINTS).assertIsDisplayed()
        rule.onNodeWithText("48 – 0").assertIsDisplayed()
        rule.onNodeWithText(UiText.STAT_DURATION).assertIsDisplayed()
        rule.onNodeWithText("01:15").assertIsDisplayed()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).assertDoesNotExist()
    }

    @Test
    fun editingNames_showsThemAndSendsThemToTheWatch() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithText(UiText.TEAM_NAMES).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.FIELD_YOU).performTextInput("Ana & Bo")
        rule.onNodeWithTag(UiTags.FIELD_OPP).performTextInput("Cy & Di")
        rule.onNodeWithTag(UiTags.NAMES_DONE).performClick()

        rule.onNodeWithText(UiText.TEAM_NAMES).assertDoesNotExist()
        rule.onNodeWithText("Ana & Bo").assertIsDisplayed()
        rule.onNodeWithText("Cy & Di").assertIsDisplayed()
        assertEquals(TeamNames("Ana & Bo", "Cy & Di"), TeamNames.decode(watch.lastSent(SyncKeys.NAMES_PATH)!!))
    }

    @Test
    fun aBlankField_keepsItsName() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithTag(UiTags.FIELD_OPP).performTextInput("Rivals")
        rule.onNodeWithTag(UiTags.NAMES_DONE).performClick()
        rule.onNodeWithText(UiText.YOU).assertIsDisplayed()
        rule.onNodeWithText("Rivals").assertIsDisplayed()
    }

    @Test
    fun cancel_keepsTheNames() {
        show()
        val sent = watch.sent.size
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithTag(UiTags.FIELD_YOU).performTextInput("Nobody")
        rule.onNodeWithTag(UiTags.NAMES_CANCEL).performClick()
        rule.onNodeWithText(UiText.TEAM_NAMES).assertDoesNotExist()
        rule.onNodeWithText(UiText.YOU).assertIsDisplayed()
        assertEquals(sent, watch.sent.size)
    }

    @Test
    fun theFields_areLabelledForTalkBack_alsoOnceThePlaceholderIsGone() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithTag(UiTags.FIELD_YOU).assertContentDescriptionEquals(UiText.YOUR_TEAM_FIELD)
        rule.onNodeWithTag(UiTags.FIELD_OPP).assertContentDescriptionEquals(UiText.OPPONENT_FIELD)
        rule.onNodeWithTag(UiTags.FIELD_YOU).performTextInput("Ana")
        rule.onNodeWithTag(UiTags.FIELD_YOU).assertContentDescriptionEquals(UiText.YOUR_TEAM_FIELD).assertTextEquals("Ana")
    }

    @Test
    fun theEditor_opensWithTheFirstFieldFocused() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithTag(UiTags.FIELD_YOU).assertIsFocused()
    }

    @Test
    fun aLongName_isCappedWhileTyping() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).performClick()
        rule.onNodeWithTag(UiTags.FIELD_YOU).performTextInput("A".repeat(TeamNames.MAX_LENGTH + 6))
        rule.onNodeWithTag(UiTags.FIELD_YOU).assertTextEquals("A".repeat(TeamNames.MAX_LENGTH))
        rule.onNodeWithTag(UiTags.NAMES_DONE).performClick()
        assertEquals("A".repeat(TeamNames.MAX_LENGTH), TeamNames.decode(watch.lastSent(SyncKeys.NAMES_PATH)!!)!!.you)
    }

    @Test
    fun everyButton_isAtLeastTheMinimumTouchTarget() {
        show()
        rule.onNodeWithTag(UiTags.EDIT_NAMES).assertMinTouchTarget().performClick()
        rule.onNodeWithTag(UiTags.NAMES_CANCEL).assertMinTouchTarget()
        rule.onNodeWithTag(UiTags.NAMES_DONE).assertMinTouchTarget()
    }

    private fun SemanticsNodeInteraction.assertMinTouchTarget() = assertWidthIsAtLeast(MinTouchTarget).assertHeightIsAtLeast(MinTouchTarget)

    @Test
    fun theNamesShowOnTheBoard() {
        val vm = show(SampleBoards.MATCH)
        rule.runOnIdle { vm.editNames("Ana", "Bo") }
        rule.onNodeWithText("Ana").assertIsDisplayed()
        rule.onNodeWithText("Bo").assertIsDisplayed()
    }
}
