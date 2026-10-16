package ch.wastedpotential.padel.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The system back, which on Wear OS is also the swipe from the left edge: it steps back through
 * the setup (SetupScreenTest), never leaves or changes a running match, and closes the app on the first step and the summary.
 */
class BackTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun duringAMatch_backIsSwallowed() {
        val vm = rule.showApp(testViewModel(singlesConfig()).apply { score(Team.YOU) })
        Espresso.pressBack()
        rule.boardPart(UiTags.SCORE_YOU).assertTextEquals("15")
        assertEquals(AppScreen.MATCH, vm.state!!.screen)
        assertFalse("the match stays in front", rule.activity.isFinishing)
    }

    @Test
    fun onTheFirstSetupStep_backLeavesTheApp() {
        rule.showApp()
        assertLeavesTheApp()
    }

    @Test
    fun onTheSummary_backLeavesTheApp() {
        rule.showApp(finishedMatch())
        assertLeavesTheApp()
    }

    private fun assertLeavesTheApp() {
        val activity = rule.activity
        Espresso.pressBackUnconditionally()
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }
}
