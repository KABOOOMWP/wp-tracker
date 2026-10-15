package ch.wastedpotential.padel.board

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.Tone
import ch.wastedpotential.padel.model.Team

/** How much the board is enlarged over the BoardMetrics reference sizes; each app provides it at its root. */
val LocalBoardScale = staticCompositionLocalOf { 1f }

/** A BoardMetrics size in dp at the current scale, never below [min]. */
@Composable @ReadOnlyComposable
fun scaledDp(size: Float, min: Float = 0f): Dp = (size * LocalBoardScale.current).coerceAtLeast(min).dp

/** A BoardMetrics font size at the current scale, never below [min]. */
@Composable @ReadOnlyComposable
fun scaledSp(size: Float, min: Float = 0f): TextUnit = (size * LocalBoardScale.current).coerceAtLeast(min).sp

/** The board's white: text, lines and icons. */
val BoardWhite: Color = Color(BoardColors.WHITE)

/** Text colour of a tone: the team accents, white for neutral. */
val Tone.accent: Color get() = when (this) {
    Tone.YOU -> Color(BoardColors.YOU_ACCENT)
    Tone.OPP -> Color(BoardColors.OPP_ACCENT)
    Tone.NEUTRAL -> BoardWhite
}

/** Background of a tone: the team panels, dark grey for neutral. */
val Tone.panel: Color get() = when (this) {
    Tone.YOU -> Color(BoardColors.YOU_PANEL)
    Tone.OPP -> Color(BoardColors.OPP_PANEL)
    Tone.NEUTRAL -> Color(BoardColors.NEUTRAL)
}

/** A team's colours are its tone's. */
val Team.accent: Color get() = Tone.of(this).accent
val Team.panel: Color get() = Tone.of(this).panel

/**
 * Board text: [size] is a BoardMetrics reference size, scaled like everything else.
 * [fit] keeps it on one line and shrinks it to half size if needed (team names).
 */
@Composable
fun BoardText(
    text: String,
    size: Float,
    color: Color,
    modifier: Modifier = Modifier,
    minSize: Float = 0f,
    weight: FontWeight = FontWeight.Bold,
    italic: Boolean = false,
    spacing: Float = 0f,
    align: TextAlign = TextAlign.Unspecified,
    fit: Boolean = false,
) {
    val fontSize = scaledSp(size, minSize)
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            letterSpacing = spacing.sp,
            textAlign = align,
        ),
        maxLines = if (fit) 1 else Int.MAX_VALUE,
        autoSize = if (fit) TextAutoSize.StepBased(minFontSize = fontSize * 0.5f, maxFontSize = fontSize) else null,
    )
}
