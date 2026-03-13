package com.wptracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.wptracker.model.*
import com.wptracker.presentation.setup.SetupScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {

    @get:Rule val rule = createComposeRule()

    // ── Step 1: Play Mode ─────────────────────────────────────────────────────

    @Test
    fun playModeStep_showsBothOptions() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("1 vs 1").assertIsDisplayed()
        rule.onNodeWithText("2 vs 2").assertIsDisplayed()
        rule.onNodeWithText("PLAY MODE").assertIsDisplayed()
    }

    @Test
    fun tapping1vs1_advancesToFormatStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }

        rule.onNodeWithText("Best of 3").assertIsDisplayed()
        rule.onNodeWithText("Best of 5").assertIsDisplayed()
        rule.onNodeWithText("FORMAT").assertIsDisplayed()
    }

    @Test
    fun tapping2vs2_advancesToFormatStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("2 vs 2").performTouchInput { click() }

        rule.onNodeWithText("Best of 3").assertIsDisplayed()
        rule.onNodeWithText("Best of 5").assertIsDisplayed()
    }

    // ── Step 2: Match Format ──────────────────────────────────────────────────

    @Test
    fun tappingBestOf3_advancesToRuleModeStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }

        rule.onNodeWithText("Standard").assertIsDisplayed()
        rule.onNodeWithText("Golden Point").assertIsDisplayed()
        rule.onNodeWithText("Star Point").assertIsDisplayed()
    }

    @Test
    fun tappingBestOf5_advancesToRuleModeStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 5").performTouchInput { click() }

        rule.onNodeWithText("Standard").assertIsDisplayed()
    }

    // ── Step 3: Rule Mode ─────────────────────────────────────────────────────

    @Test
    fun tappingStandard_advancesToWhoServesStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        advanceToRuleMode()
        rule.onNodeWithText("Standard").performTouchInput { click() }

        rule.onNodeWithText("OPPONENT").assertIsDisplayed()
        rule.onNodeWithText("YOU").assertIsDisplayed()
        rule.onNodeWithText("WHO SERVES?").assertIsDisplayed()
    }

    @Test
    fun tappingGoldenPoint_advancesToWhoServesStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        advanceToRuleMode()
        rule.onNodeWithText("Golden Point").performTouchInput { click() }

        rule.onNodeWithText("WHO SERVES?").assertIsDisplayed()
    }

    @Test
    fun tappingStarPoint_advancesToWhoServesStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        advanceToRuleMode()
        rule.onNodeWithText("Star Point").performTouchInput { click() }

        rule.onNodeWithText("WHO SERVES?").assertIsDisplayed()
    }

    // ── Step 4 → onMatchStart (singles) ──────────────────────────────────────

    @Test
    fun singlesFlow_youServe_callsOnMatchStartWithCorrectConfig() {
        var received: Config? = null
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = { received = it }) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
        rule.onNodeWithText("Standard").performTouchInput { click() }
        rule.onNodeWithText("YOU").performTouchInput { click() }

        val config = checkNotNull(received)
        assertEquals(PlayMode.SINGLES, config.playMode)
        assertEquals(3, config.bestOf)
        assertEquals(RuleMode.STANDARD, config.ruleMode)
        assertEquals(Player.A1, config.serveOrder.first())
    }

    @Test
    fun singlesFlow_oppServe_callsOnMatchStartWithOppFirst() {
        var received: Config? = null
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = { received = it }) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
        rule.onNodeWithText("Standard").performTouchInput { click() }
        rule.onNodeWithText("OPPONENT").performTouchInput { click() }

        assertEquals(Player.B1, checkNotNull(received).serveOrder.first())
    }

    @Test
    fun singlesFlow_bestOf5_goldenPoint_producesCorrectConfig() {
        var received: Config? = null
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = { received = it }) } }

        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 5").performTouchInput { click() }
        rule.onNodeWithText("Golden Point").performTouchInput { click() }
        rule.onNodeWithText("YOU").performTouchInput { click() }

        val config = checkNotNull(received)
        assertEquals(5, config.bestOf)
        assertEquals(RuleMode.GOLDEN, config.ruleMode)
    }

    // ── Step 5: Which Player (doubles) ────────────────────────────────────────

    @Test
    fun doublesFlow_advancesToWhichPlayerStep() {
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = {}) } }

        rule.onNodeWithText("2 vs 2").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
        rule.onNodeWithText("Standard").performTouchInput { click() }
        rule.onNodeWithText("YOU").performTouchInput { click() }

        rule.onNodeWithText("← LEFT").assertIsDisplayed()
        rule.onNodeWithText("RIGHT →").assertIsDisplayed()
        rule.onNodeWithText("WHO SERVES?").assertIsDisplayed()
        rule.onNodeWithText("YOUR SIDE").assertIsDisplayed()
    }

    @Test
    fun doublesFlow_leftPick_callsOnMatchStartWithDoublesConfig() {
        var received: Config? = null
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = { received = it }) } }

        rule.onNodeWithText("2 vs 2").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
        rule.onNodeWithText("Standard").performTouchInput { click() }
        rule.onNodeWithText("YOU").performTouchInput { click() }
        rule.onNodeWithText("← LEFT").performTouchInput { click() }

        val left = checkNotNull(received)
        assertEquals(PlayMode.DOUBLES, left.playMode)
        assertEquals(4, left.serveOrder.size)
    }

    @Test
    fun doublesFlow_rightPick_callsOnMatchStartWithDoublesConfig() {
        var received: Config? = null
        rule.setContent { TestWrapper { SetupScreen(onMatchStart = { received = it }) } }

        rule.onNodeWithText("2 vs 2").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
        rule.onNodeWithText("Standard").performTouchInput { click() }
        rule.onNodeWithText("YOU").performTouchInput { click() }
        rule.onNodeWithText("RIGHT →").performTouchInput { click() }

        val right = checkNotNull(received)
        assertEquals(PlayMode.DOUBLES, right.playMode)
        assertEquals(4, right.serveOrder.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun advanceToRuleMode() {
        rule.onNodeWithText("1 vs 1").performTouchInput { click() }
        rule.onNodeWithText("Best of 3").performTouchInput { click() }
    }
}
