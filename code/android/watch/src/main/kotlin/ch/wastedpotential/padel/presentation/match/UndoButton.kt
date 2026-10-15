package ch.wastedpotential.padel.presentation.match

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import ch.wastedpotential.padel.board.BoardWhite
import ch.wastedpotential.padel.board.scaledDp
import ch.wastedpotential.padel.board.strokeIcon
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.BoardMetrics
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.display.UiTimings
import kotlinx.coroutines.launch

/** Tap = undo (arrow) or end the match (X). Holding for [holdMs] fills a ring and toggles between the two. */
@Composable
internal fun UndoButton(
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
    modifier: Modifier = Modifier,
    holdMs: Long = UiTimings.END_MATCH_HOLD_MS,
) {
    var endMode by remember { mutableStateOf(false) }
    val ring = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val undo by rememberUpdatedState(onUndo)
    val endMatch by rememberUpdatedState(onEndMatch)
    val tap = { if (endMode) endMatch() else undo() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(scaledDp(BoardMetrics.UNDO_BUTTON, min = BoardMetrics.UNDO_BUTTON_MIN))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = if (endMode) UiText.END_MATCH else UiText.UNDO) {
                    tap()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val hold = scope.launch {
                        ring.animateTo(1f, tween(holdMs.toInt(), easing = LinearEasing))
                        endMode = !endMode
                        ring.snapTo(0f)
                    }
                    val released = waitForUpOrCancellation() != null
                    val wasTap = released && hold.isActive
                    hold.cancel()
                    scope.launch { ring.snapTo(0f) }
                    if (wasTap) tap()
                }
            },
    ) {
        val ringWidth = scaledDp(BoardMetrics.UNDO_RING, min = BoardMetrics.UNDO_RING_MIN)
        val ringPad = scaledDp(BoardMetrics.UNDO_RING_PAD)
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(BoardColors.UNDO))
            if (ring.value > 0f) {
                val pad = ringPad.toPx()
                drawArc(
                    color = BoardWhite,
                    startAngle = -90f,
                    sweepAngle = 360f * ring.value,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2, size.height - pad * 2),
                    style = Stroke(width = ringWidth.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Image(
            imageVector = if (endMode) CloseX else ReverseArrow,
            contentDescription = if (endMode) UiText.END_MATCH else UiText.UNDO,
            colorFilter = ColorFilter.tint(Color(if (endMode) BoardColors.END_MATCH else BoardColors.OPP_ACCENT)),
            modifier = Modifier.size(scaledDp(if (endMode) BoardMetrics.END_ICON else BoardMetrics.UNDO_ICON)),
        )
    }
}

private val ReverseArrow = strokeIcon("ReverseArrow", BoardMetrics.UNDO_ICON_STROKE) {
    moveTo(9f, 15f)
    lineTo(3f, 9f)
    moveTo(3f, 9f)
    lineTo(9f, 3f)
    moveTo(3f, 9f)
    lineTo(15f, 9f)
    arcToRelative(6f, 6f, 0f, false, true, 0f, 12f)
    lineTo(12f, 21f)
}

private val CloseX = strokeIcon("CloseX", BoardMetrics.END_ICON_STROKE) {
    moveTo(6f, 18f)
    lineTo(18f, 6f)
    moveTo(6f, 6f)
    lineTo(18f, 18f)
}
