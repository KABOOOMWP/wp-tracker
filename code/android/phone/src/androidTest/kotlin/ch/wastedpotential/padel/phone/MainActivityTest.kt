package ch.wastedpotential.padel.phone

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.sync.SampleBoards
import org.junit.Rule
import org.junit.Test

/** The real activity, started with a sample board so it runs without a watch. */
class MainActivityTest {

    @get:Rule val rule = createEmptyComposeRule()

    private fun launch(sample: String) = ActivityScenario.launch<MainActivity>(
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SAMPLE_BOARD, sample),
    )

    @Test
    fun sampleMatch_isShownLandscapeAndKeepsTheScreenOn() {
        launch(SampleBoards.MATCH).use { scenario ->
            rule.onNodeWithTag(UiTags.SCORE_YOU, useUnmergedTree = true).assertTextEquals("40")
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                check(flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0) { "screen may sleep" }
                check(activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    "not landscape"
                }
            }
        }
    }

    @Test
    fun sampleSummary_isShown() {
        launch(SampleBoards.SUMMARY).use {
            rule.onNodeWithText(UiText.YOU_WIN).assertExists()
        }
    }
}
