package ch.wastedpotential.padel.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.TeamNames
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SummaryScreenTest {

    @get:Rule val rule = createComposeRule()

    private val you = TeamNames.DEFAULT.summaryName(Team.YOU)
    private val opp = TeamNames.DEFAULT.summaryName(Team.OPP)

    // A result panel reads as one item: the name, the games of each set, then the sets won

    @Test
    fun showsHeaderAndNewMatch() {
        rule.showApp(finishedMatch())
        rule.onNodeWithText(UiText.MATCH_SUMMARY).assertIsDisplayed()
        rule.onNodeWithText(UiText.NEW_MATCH).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun youWin_eachPanelShowsItsSetsAndSetsWon() {
        rule.showApp(finishedMatch(winner = Team.YOU))
        rule.onNodeWithText(UiText.YOU_WIN).assertIsDisplayed()
        rule.onNode(hasText(opp)).assertTextEquals(opp, "0", "0", "0")
        rule.onNode(hasText(you) and hasText("2")).assertTextEquals(you, "6", "6", "2")
    }

    @Test
    fun oppWin_showsOpponentWinsAndItsSets() {
        rule.showApp(finishedMatch(winner = Team.OPP))
        rule.onNodeWithText(UiText.OPPONENT_WINS).assertIsDisplayed()
        rule.onNode(hasText(opp)).assertTextEquals(opp, "6", "6", "2")
    }

    @Test
    fun showsEveryStatRow() {
        rule.showApp(finishedMatch())
        listOf(UiText.STAT_POINTS to "48 – 0", UiText.STAT_GAMES to "12 – 0", UiText.STAT_BREAKS to "6 – 0").forEach { (label, value) ->
            rule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(value).assertIsDisplayed()
        }
        rule.onNodeWithText(UiText.STAT_DURATION).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(UiText.STAT_AVG_POINTS).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(UiText.STAT_DEUCES).assertDoesNotExist()
    }

    @Test
    fun goldenMode_showsGoldenWonRow_whenDecidersWerePlayed() {
        val vm = testViewModel(singlesConfig(ruleMode = RuleMode.GOLDEN)).apply {
            repeat(3) {
                score(Team.YOU)
                score(Team.OPP)
            }
            score(Team.YOU)
            end()
        }
        rule.showApp(vm)
        rule.onNodeWithText(UiText.STAT_GOLDEN_WON).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 / 1").assertIsDisplayed()
    }

    @Test
    fun tappingNewMatch_returnsToSetup() {
        val vm = rule.showApp(finishedMatch())
        rule.onNodeWithTag(UiTags.NEW_MATCH).performScrollTo().performClick()
        assertEquals(AppScreen.SETUP, vm.state!!.screen)
        rule.onNodeWithText(UiText.PLAY_MODE).assertIsDisplayed()
    }

    @Test
    fun wonMatch_undoLastPoint_reopensTheMatch() {
        val vm = rule.showApp(finishedMatch())
        rule.onNodeWithText(UiText.UNDO_LAST_POINT).performScrollTo().performClick()
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
        rule.boardPart(UiTags.SCORE_YOU).assertTextEquals("40")
    }

    @Test
    fun stoppedMatch_offersResumeAndHasNoWinner() {
        val vm = testViewModel(singlesConfig()).apply {
            repeat(5) { score(Team.YOU) }
            end()
        }
        rule.showApp(vm)
        rule.onNodeWithText(UiText.MATCH_ENDED).assertIsDisplayed()
        rule.onNodeWithText(UiText.RESUME_MATCH).performScrollTo().performClick()
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
        rule.boardPart(UiTags.SCORE_YOU).assertTextEquals("15")
    }
}
