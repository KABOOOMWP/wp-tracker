package ch.wastedpotential.padel.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.advanceEventTime
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.presentation.WatchFrame
import ch.wastedpotential.padel.presentation.match.UndoButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The hold that turns undo into end match, on the test's own clock and with its own hold time. */
class UndoButtonTest {

    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show() = rule.setContent {
        WatchFrame { UndoButton({ calls += UiText.UNDO }, { calls += UiText.END_MATCH }, Modifier.testTag(UiTags.UNDO), HOLD_MS) }
    }

    private fun press(ms: Long) = rule.onNodeWithTag(UiTags.UNDO).performTouchInput {
        down(center)
        advanceEventTime(ms)
        up()
    }

    @Test
    fun aShortPressUndoes() {
        show()
        press(HOLD_MS - MARGIN_MS)
        rule.onNodeWithContentDescription(UiText.UNDO).assertIsDisplayed()
        assertEquals(listOf(UiText.UNDO), calls)
    }

    @Test
    fun aFullHoldTurnsItIntoEndMatch_andATapThenEnds() {
        show()
        press(HOLD_MS + MARGIN_MS)
        assertEquals("the hold itself does nothing", emptyList<String>(), calls)
        rule.onNodeWithContentDescription(UiText.END_MATCH).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.UNDO).performTouchInput { click() }
        assertEquals(listOf(UiText.END_MATCH), calls)
    }

    @Test
    fun aSecondHoldTurnsItBack() {
        show()
        press(HOLD_MS + MARGIN_MS)
        press(HOLD_MS + MARGIN_MS)
        rule.onNodeWithContentDescription(UiText.UNDO).assertIsDisplayed()
        rule.onNodeWithTag(UiTags.UNDO).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(UiText.UNDO), calls)
    }

    private companion object {
        const val HOLD_MS = 600L

        /** A few frames either side of the hold. */
        const val MARGIN_MS = 200L
    }
}
