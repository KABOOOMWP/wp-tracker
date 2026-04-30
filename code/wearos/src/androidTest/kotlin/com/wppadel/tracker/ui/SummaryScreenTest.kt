package com.wppadel.tracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.wppadel.tracker.model.*
import com.wppadel.tracker.presentation.summary.SummaryScreen
import org.junit.Rule
import org.junit.Test

class SummaryScreenTest {

    @get:Rule val rule = createComposeRule()

    // ── Header & structure ────────────────────────────────────────────────────

    @Test
    fun showsMatchSummaryHeader() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("MATCH SUMMARY").assertIsDisplayed()
    }

    @Test
    fun showsTeamLabels() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        // "YOU" appears in both the panel label and the stats column header — use first match
        rule.onAllNodesWithText("YOU").onFirst().assertIsDisplayed()
        rule.onNodeWithText("OPPONENT").assertIsDisplayed()
    }

    @Test
    fun showsNewMatchButton() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("NEW MATCH").performScrollTo().assertIsDisplayed()
    }

    // ── Winner pill ───────────────────────────────────────────────────────────

    @Test
    fun youWin_showsYouWinPill() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("YOU WIN").assertIsDisplayed()
    }

    @Test
    fun oppWin_showsOpponentWinsPill() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = oppWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("OPPONENT WINS").assertIsDisplayed()
    }

    // ── Set scores ────────────────────────────────────────────────────────────

    @Test
    fun showsSetsWonCount_forYou() {
        val snap = youWinSnapshot()
        rule.setContent { TestWrapper { SummaryScreen(snapshot = snap, onNewMatch = {}) } }

        // 2-0 match: YOU shows "2" as total sets won
        rule.onAllNodesWithText("2").onFirst().assertIsDisplayed()
    }

    @Test
    fun showsSetsWonCount_forOpp() {
        val snap = youWinSnapshot()
        rule.setContent { TestWrapper { SummaryScreen(snapshot = snap, onNewMatch = {}) } }

        // 2-0 match: OPP shows "0" as total sets won
        rule.onAllNodesWithText("0").onFirst().assertIsDisplayed()
    }

    // ── Stats rows ────────────────────────────────────────────────────────────

    @Test
    fun showsPointsStatRow() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("Points").assertIsDisplayed()
    }

    @Test
    fun showsGamesStatRow() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("Games").assertIsDisplayed()
    }

    @Test
    fun showsBreaksStatRow() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("Breaks").assertIsDisplayed()
    }

    @Test
    fun showsDurationStatRow() {
        rule.setContent { TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = {}) } }

        rule.onNodeWithText("Duration").performScrollTo().assertIsDisplayed()
    }

    // ── Golden/Star stats rows ─────────────────────────────────────────────────

    @Test
    fun goldenMode_showsGoldenWonRow_whenDecidersPlayed() {
        val snap = completedMatch(config = singlesConfig(ruleMode = RuleMode.GOLDEN))
        val withDeciders = snap.copy(
            stats = snap.stats.copy(goldenDecidersPlayed = 2, goldenDecidersWonYou = 1)
        )
        rule.setContent { TestWrapper { SummaryScreen(snapshot = withDeciders, onNewMatch = {}) } }

        rule.onNodeWithText("Golden won").performScrollTo().assertIsDisplayed()
    }

    // ── New match callback ────────────────────────────────────────────────────

    @Test
    fun tappingNewMatch_callsOnNewMatch() {
        var called = false
        rule.setContent {
            TestWrapper { SummaryScreen(snapshot = youWinSnapshot(), onNewMatch = { called = true }) }
        }

        rule.onNodeWithText("NEW MATCH").performScrollTo().performClick()

        assert(called) { "onNewMatch was not called" }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun youWinSnapshot() = completedMatch(config = singlesConfig(), winner = Team.YOU)
    private fun oppWinSnapshot() = completedMatch(config = singlesConfig(), winner = Team.OPP)
}
