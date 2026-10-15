package ch.wastedpotential.padel.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import ch.wastedpotential.padel.display.Prompt
import ch.wastedpotential.padel.display.PromptKind
import ch.wastedpotential.padel.display.Tone
import ch.wastedpotential.padel.presentation.WatchFrame
import ch.wastedpotential.padel.presentation.prompt.PromptView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PromptViewTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun aTapPromptWithoutChoices_stillDrawsAndAnswersZero() {
        val answers = mutableListOf<Int>()
        rule.setContent {
            WatchFrame { PromptView(Prompt(PromptKind.TAP, "HEADER", "hint", Tone.NEUTRAL, emptyList()), answers::add) }
        }
        rule.onNodeWithText("hint").assertIsDisplayed()
        rule.onNodeWithText("HEADER").performTouchInput { click() }
        assertEquals(listOf(0), answers)
    }
}
