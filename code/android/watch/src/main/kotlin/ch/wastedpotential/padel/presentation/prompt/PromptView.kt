package ch.wastedpotential.padel.presentation.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import ch.wastedpotential.padel.board.BoardText
import ch.wastedpotential.padel.board.BoardWhite
import ch.wastedpotential.padel.board.accent
import ch.wastedpotential.padel.board.panel
import ch.wastedpotential.padel.board.scaledDp
import ch.wastedpotential.padel.display.BoardAlpha
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.Choice
import ch.wastedpotential.padel.display.Prompt
import ch.wastedpotential.padel.display.PromptKind
import ch.wastedpotential.padel.display.Tone
import ch.wastedpotential.padel.display.BoardMetrics as M

private val Black = Color(BoardColors.BACKGROUND)

/** A full-screen question; [onChoose] gets the answer index (0 = left, or the only answer of a tap prompt). */
@Composable
fun PromptView(prompt: Prompt, onChoose: (Int) -> Unit) = when (prompt.kind) {
    PromptKind.TAP -> TapPrompt(prompt, onChoose)
    PromptKind.LEFT_RIGHT -> LeftRightPicker(prompt, onChoose)
}

/** Any tap answers 0; a prompt without choices still draws (it has no tag then). */
@Composable
private fun TapPrompt(prompt: Prompt, onChoose: (Int) -> Unit) {
    val choice = prompt.choices.firstOrNull()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .clickable(interactionSource = null, indication = null, onClickLabel = choice?.label, role = Role.Button) { onChoose(0) }
            .then(if (choice != null) Modifier.testTag(choice.tag) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaledDp(M.PROMPT_GAP, min = M.PROMPT_GAP_MIN)),
            modifier = Modifier.padding(horizontal = scaledDp(M.PROMPT_PAD)),
        ) {
            BoardText(
                prompt.header,
                M.PROMPT_TITLE,
                prompt.headerTone.accent,
                minSize = M.PROMPT_TITLE_MIN,
                spacing = 0.5f,
                align = TextAlign.Center,
            )
            BoardText(
                prompt.subtitle,
                M.PROMPT_HINT,
                BoardWhite.copy(alpha = BoardAlpha.HINT),
                minSize = M.PROMPT_HINT_MIN,
                spacing = 0.5f,
                align = TextAlign.Center,
            )
        }
    }
}

/** Left half answers 0, right half answers 1; the question sits on top. */
@Composable
private fun LeftRightPicker(prompt: Prompt, onChoose: (Int) -> Unit) {
    Box(Modifier.fillMaxSize().background(Black.copy(alpha = BoardAlpha.SCRIM))) {
        Row(Modifier.fillMaxSize()) {
            prompt.choices.take(2).forEachIndexed { i, choice -> PickerHalf(choice) { onChoose(i) } }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Black)
                .padding(
                    top = scaledDp(M.PICKER_BAND_TOP),
                    start = scaledDp(M.PICKER_BAND_SIDE),
                    end = scaledDp(M.PICKER_BAND_SIDE),
                    bottom = scaledDp(M.PICKER_BAND_BOTTOM),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaledDp(M.PICKER_BAND_GAP)),
        ) {
            val headerColor = when (prompt.headerTone) {
                Tone.NEUTRAL -> BoardWhite.copy(alpha = BoardAlpha.HEADER)
                else -> prompt.headerTone.accent
            }
            BoardText(prompt.header, M.HEADER_FONT, headerColor, minSize = M.HEADER_FONT_MIN, spacing = 0.5f, align = TextAlign.Center)
            BoardText(
                prompt.subtitle,
                M.SUBTITLE_FONT,
                BoardWhite.copy(alpha = BoardAlpha.SUBTITLE),
                minSize = M.SUBTITLE_FONT_MIN,
                spacing = 0.5f,
                align = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RowScope.PickerHalf(choice: Choice, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f).fillMaxHeight()
            .background(choice.tone.panel)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .testTag(choice.tag),
    ) {
        BoardText(choice.label, M.PICKER_FONT, BoardWhite, minSize = M.PICKER_FONT_MIN)
    }
}
