package com.wppadel.tracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.wppadel.tracker.haptic.HapticManager
import com.wppadel.tracker.model.*
import com.wppadel.tracker.presentation.match.MatchScreen
import com.wppadel.tracker.presentation.match.MatchViewModel
import org.junit.Rule
import org.junit.Test

class MatchScreenTest {

    @get:Rule val rule = createComposeRule()

    private val ctx    get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val haptic get() = HapticManager(ctx)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_bothScoresAreZero() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("score_you").assertTextEquals("0")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    @Test
    fun initialState_teamLabelsAreVisible() {
        setMatchContent(singlesConfig())

        rule.onNodeWithText("You").assertIsDisplayed()
        rule.onNodeWithText("Opponent").assertIsDisplayed()
    }

    @Test
    fun initialState_undoButtonIsVisible() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("btn_undo").assertIsDisplayed()
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    @Test
    fun tappingYouZone_incrementsYourScore() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("tap_you").performTouchInput { click() }

        rule.onNodeWithTag("score_you").assertTextEquals("15")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    @Test
    fun tappingOppZone_incrementsOpponentScore() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("tap_opp").performTouchInput { click() }

        rule.onNodeWithTag("score_opp").assertTextEquals("15")
        rule.onNodeWithTag("score_you").assertTextEquals("0")
    }

    @Test
    fun scoringSequence_followsStandardPointProgression() {
        setMatchContent(singlesConfig())

        // 0 → 15 → 30 → 40
        repeat(3) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }

        rule.onNodeWithTag("score_you").assertTextEquals("40")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    @Test
    fun tieBreak_displaysRawPointCounts() {
        setMatchContent(singlesConfig())

        // Alternate game wins to reach 6-6 within the same set:
        // 5 rounds of (YOU wins, OPP wins) → 5-5, then YOU wins → 6-5, OPP wins → 6-6
        repeat(5) {
            repeat(4) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
            repeat(4) { rule.onNodeWithTag("tap_opp").performTouchInput { click() } }
        }
        repeat(4) { rule.onNodeWithTag("tap_you").performTouchInput { click() } } // 6-5
        repeat(4) { rule.onNodeWithTag("tap_opp").performTouchInput { click() } } // 6-6 → TIE-BREAK

        rule.onNodeWithText("TIE-BREAK").assertIsDisplayed()
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_afterOnePoint_revertsToZero() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("tap_you").performTouchInput { click() }
        rule.onNodeWithTag("score_you").assertTextEquals("15")

        rule.onNodeWithTag("btn_undo").performTouchInput { click() }

        rule.onNodeWithTag("score_you").assertTextEquals("0")
    }

    @Test
    fun undo_multiplePoints_revertsCorrectly() {
        setMatchContent(singlesConfig())

        rule.onNodeWithTag("tap_you").performTouchInput { click() } // 15
        rule.onNodeWithTag("tap_you").performTouchInput { click() } // 30
        rule.onNodeWithTag("btn_undo").performTouchInput { click() } // back to 15

        rule.onNodeWithTag("score_you").assertTextEquals("15")
    }

    @Test
    fun undo_atStart_doesNothing() {
        setMatchContent(singlesConfig())

        // No points scored — undo should be a no-op
        rule.onNodeWithTag("btn_undo").performTouchInput { click() }

        rule.onNodeWithTag("score_you").assertTextEquals("0")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    // ── Status pill ───────────────────────────────────────────────────────────

    @Test
    fun matchPoint_pillIsShown_whenOnePointFromMatchWin() {
        // Best of 1 set isn't valid (bestOf = 3), so we need to reach match point:
        // Bring YOU to 5-0 in set 1 (one game from set win = potential match point
        // only if it's also a break point, so let's just get to 6-0 5-0 game 5 score 40-0)
        val config = singlesConfig(bestOf = 3)
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        // Win set 1: 24 YOU points
        repeat(24) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
        // In set 2 reach 5-0 (20 YOU points)
        repeat(20) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
        // Now score 3 more points in the game to reach 40-0 (match/set point)
        repeat(3) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }

        rule.onNodeWithText("MATCH POINT").assertIsDisplayed()
    }

    @Test
    fun goldenPoint_pillIsShown_atDeuce_inGoldenMode() {
        val config = singlesConfig(ruleMode = RuleMode.GOLDEN)
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        // Reach deuce: YOU 3 pts, OPP 3 pts
        repeat(3) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
        repeat(3) { rule.onNodeWithTag("tap_opp").performTouchInput { click() } }

        rule.onNodeWithText("GOLDEN POINT").assertIsDisplayed()
        rule.onNodeWithText("SERVE FROM").assertDoesNotExist()
    }

    @Test
    fun goldenPoint_canScoreWithoutDeciderPick() {
        val config = singlesConfig(ruleMode = RuleMode.GOLDEN)
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        // Reach golden point (3:3)
        repeat(3) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
        repeat(3) { rule.onNodeWithTag("tap_opp").performTouchInput { click() } }

        // Score the deciding point directly — no picker interaction needed
        rule.onNodeWithTag("tap_you").performTouchInput { click() }

        // New game starts with 0:0
        rule.onNodeWithTag("score_you").assertTextEquals("0")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    // ── Serve pick overlay (doubles) ──────────────────────────────────────────

    @Test
    fun doublesGame2_showsServePickOverlay() {
        val config = doublesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        // Win game 1 (4 points for YOU) — game 2 should show serve pick overlay
        repeat(4) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }

        rule.onNodeWithText("WHO SERVES?").assertIsDisplayed()
        rule.onNodeWithText("← LEFT").assertIsDisplayed()
        rule.onNodeWithText("RIGHT →").assertIsDisplayed()
    }

    // ── Position-switch overlay (doubles, after each non-final set) ──────────

    @Test
    fun doublesAfterSet1_showsYourTeamPositionSwitchOverlay() {
        val config = doublesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        playDoublesSetYou()

        rule.onNodeWithText("SWITCH SIDES?").assertIsDisplayed()
        rule.onNodeWithText("YOUR TEAM").assertIsDisplayed()
    }

    @Test
    fun doublesAfterSet1_afterYouAnswer_showsOppPositionSwitchOverlay() {
        val config = doublesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        playDoublesSetYou()
        rule.onNodeWithTag("btn_position_switch_yes").performTouchInput { click() }

        rule.onNodeWithText("SWITCH SIDES?").assertIsDisplayed()
        rule.onNodeWithText("OPP TEAM").assertIsDisplayed()
    }

    @Test
    fun doublesAfterSet1_afterBothAnswerKeep_matchContinues() {
        val config = doublesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        playDoublesSetYou()
        rule.onNodeWithTag("btn_position_switch_no").performTouchInput { click() }  // YOU: keep
        rule.onNodeWithTag("btn_position_switch_no").performTouchInput { click() }  // OPP: keep

        // Overlay gone, set 2 has started with both game scores reset
        rule.onNodeWithTag("score_you").assertTextEquals("0")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    @Test
    fun doublesAfterSet1_afterBothAnswerSwitch_matchContinues() {
        val config = doublesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        playDoublesSetYou()
        rule.onNodeWithTag("btn_position_switch_yes").performTouchInput { click() }  // YOU: switch
        rule.onNodeWithTag("btn_position_switch_yes").performTouchInput { click() }  // OPP: switch

        rule.onNodeWithTag("score_you").assertTextEquals("0")
        rule.onNodeWithTag("score_opp").assertTextEquals("0")
    }

    @Test
    fun singlesAfterSet1_doesNotShowPositionSwitchOverlay() {
        val config = singlesConfig()
        val vm = MatchViewModel().also { it.init(config) }
        setMatchContent(config, vm)

        repeat(24) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }

        rule.onNodeWithText("SWITCH SIDES?").assertDoesNotExist()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Plays a complete 6-0 set for YOU in a doubles match.
     * After game 1 the serve-pick overlay appears (OPP must choose their game-2 server);
     * this helper dismisses it by tapping RIGHT, then scores the remaining 5 games.
     */
    private fun playDoublesSetYou() {
        repeat(4) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
        rule.onNodeWithText("RIGHT →").performTouchInput { click() }  // dismiss serve-pick overlay
        repeat(20) { rule.onNodeWithTag("tap_you").performTouchInput { click() } }
    }

    private fun setMatchContent(config: Config, vm: MatchViewModel = MatchViewModel()) {
        rule.setContent {
            TestWrapper {
                MatchScreen(
                    config     = config,
                    haptic     = haptic,
                    onMatchEnd = {},
                    vm         = vm
                )
            }
        }
        rule.waitForIdle()
    }
}
