package ch.wastedpotential.padel.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ch.wastedpotential.padel.board.BoardText
import ch.wastedpotential.padel.board.PillDivider
import ch.wastedpotential.padel.board.accent
import ch.wastedpotential.padel.board.panel
import ch.wastedpotential.padel.display.BoardMetrics
import ch.wastedpotential.padel.match.MatchSession
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.presentation.WATCH_PREVIEW
import ch.wastedpotential.padel.presentation.WatchFrame
import ch.wastedpotential.padel.presentation.prompt.PromptView
import ch.wastedpotential.padel.setup.SetupScreen

/** One setup step: its left/right picker, or its stack of choices with a pill on each divider. */
@Composable
fun SetupView(screen: SetupScreen, onChoose: (Int) -> Unit) {
    val picker = screen.picker
    if (picker != null) {
        PromptView(picker, onChoose)
    } else {
        Column(Modifier.fillMaxSize()) {
            screen.choices.forEachIndexed { i, choice ->
                if (i > 0) PillDivider(screen.pills.getOrNull(i - 1))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth().weight(1f)
                        .background(choice.tone.panel)
                        .clickable(interactionSource = null, indication = null, role = Role.Button) { onChoose(i) }
                        .testTag(choice.tag),
                ) {
                    BoardText(choice.label, BoardMetrics.CHOICE_FONT, choice.tone.accent)
                }
            }
        }
    }
}

@Preview(device = WATCH_PREVIEW)
@Composable
private fun SetupViewPreview() = WatchFrame {
    SetupView(MatchSession(MemoryTextStore(), null).state.setup!!) {}
}
