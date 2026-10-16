package ch.wastedpotential.padel.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.model.Team
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun clickText(vararg labels: String) = labels.forEach { rule.onNodeWithText(it).performTouchInput { click() } }

    @Test
    fun playModeStep_showsBothOptions() {
        rule.showApp()
        rule.onNodeWithText(UiText.SINGLES).assertIsDisplayed()
        rule.onNodeWithText(UiText.DOUBLES).assertIsDisplayed()
        rule.onNodeWithText(UiText.PLAY_MODE).assertIsDisplayed()
    }

    @Test
    fun playMode_advancesToFormat() {
        rule.showApp()
        clickText(UiText.DOUBLES)
        rule.onNodeWithText(UiText.BEST_OF_3).assertIsDisplayed()
        rule.onNodeWithText(UiText.BEST_OF_5).assertIsDisplayed()
        rule.onNodeWithText(UiText.FORMAT).assertIsDisplayed()
    }

    @Test
    fun format_advancesToRuleMode_withThreeChoicesAndTwoPills() {
        rule.showApp()
        clickText(UiText.SINGLES, UiText.BEST_OF_5)
        rule.onNodeWithText(UiText.RULE_STANDARD).assertIsDisplayed()
        rule.onNodeWithText(UiText.RULE_GOLDEN).assertIsDisplayed()
        rule.onNodeWithText(UiText.RULE_STAR).assertIsDisplayed()
        rule.onNodeWithText(UiText.GAME).assertIsDisplayed()
        rule.onNodeWithText(UiText.MODE).assertIsDisplayed()
    }

    @Test
    fun ruleMode_advancesToWhoServes() {
        rule.showApp()
        clickText(UiText.SINGLES, UiText.BEST_OF_3, UiText.RULE_GOLDEN)
        rule.onNodeWithText(UiText.SERVE_OPP).assertIsDisplayed()
        rule.onNodeWithText(UiText.SERVE_YOU).assertIsDisplayed()
        rule.onNodeWithText(UiText.WHO_SERVES).assertIsDisplayed()
    }

    @Test
    fun singles_youServe_startsTheMatch() {
        val vm = rule.showApp()
        clickText(UiText.SINGLES, UiText.BEST_OF_3, UiText.RULE_STANDARD, UiText.SERVE_YOU)
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
        assertEquals(Team.YOU, vm.state!!.display!!.servingTeam)
        rule.boardPart(UiTags.SCORE_YOU).assertTextEquals("0")
    }

    @Test
    fun singles_opponentServes_startsWithTheOpponentServing() {
        val vm = rule.showApp()
        clickText(UiText.SINGLES, UiText.BEST_OF_5, UiText.RULE_STAR, UiText.SERVE_OPP)
        assertEquals(Team.OPP, vm.state!!.display!!.servingTeam)
    }

    @Test
    fun doubles_asksWhichPlayerServes_thenStarts() {
        val vm = rule.showApp()
        clickText(UiText.DOUBLES, UiText.BEST_OF_3, UiText.RULE_STANDARD, UiText.SERVE_YOU)
        rule.onNodeWithText(UiText.WHO_SERVES).assertIsDisplayed()
        rule.onNodeWithText(UiText.YOUR_SIDE).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.SERVER_LEFT).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.SERVER_RIGHT).performTouchInput { click() }
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
    }

    @Test
    fun doubles_leftServer_putsTheStripeLeft() {
        val vm = rule.showApp()
        clickText(UiText.DOUBLES, UiText.BEST_OF_3, UiText.RULE_STANDARD, UiText.SERVE_OPP)
        rule.onNodeWithText(UiText.OPPONENT_SIDE).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.SERVER_LEFT).performTouchInput { click() }
        assertEquals(true, vm.state!!.display!!.serveStripeOnLeft)
    }

    @Test
    fun back_stepsBackThroughTheSetup() {
        rule.showApp()
        clickText(UiText.SINGLES, UiText.BEST_OF_3)
        rule.onNodeWithText(UiText.RULE_STANDARD).assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithText(UiText.BEST_OF_3).assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithText(UiText.SINGLES).assertIsDisplayed()
    }
}
