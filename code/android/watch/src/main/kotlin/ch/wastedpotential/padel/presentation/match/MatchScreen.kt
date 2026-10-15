package ch.wastedpotential.padel.presentation.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ch.wastedpotential.padel.board.MatchBoard
import ch.wastedpotential.padel.board.scaledDp
import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.presentation.LocalIsRoundScreen
import ch.wastedpotential.padel.presentation.WATCH_PREVIEW
import ch.wastedpotential.padel.presentation.WatchFrame
import ch.wastedpotential.padel.presentation.prompt.PromptView
import ch.wastedpotential.padel.sync.SampleBoards
import ch.wastedpotential.padel.sync.TeamNames
import ch.wastedpotential.padel.display.BoardMetrics as M

/** Tap the top half for the opponent, the bottom half for you; undo on the right; prompts cover it all. */
@Composable
fun MatchScreen(
    display: MatchDisplay,
    names: TeamNames,
    onScore: (Team) -> Unit,
    onUndo: () -> Unit,
    onEndMatch: () -> Unit,
    onAnswer: (Int) -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val hInset = scaledDp(if (LocalIsRoundScreen.current) M.H_INSET_ROUND else M.H_INSET)
    // TalkBack reaches every action from either zone, ending included: the hold gesture needs sight
    val actions = listOf(
        CustomAccessibilityAction(UiText.pointFor(names.name(Team.YOU))) {
            onScore(Team.YOU)
            true
        },
        CustomAccessibilityAction(UiText.pointFor(names.name(Team.OPP))) {
            onScore(Team.OPP)
            true
        },
        CustomAccessibilityAction(UiText.UNDO) {
            onUndo()
            true
        },
        CustomAccessibilityAction(UiText.END_MATCH) {
            onEndMatch()
            true
        },
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TapZone(UiTags.TAP_OPP, UiText.pointFor(names.name(Team.OPP)), actions) { onScore(Team.OPP) }
            TapZone(UiTags.TAP_YOU, UiText.pointFor(names.name(Team.YOU)), actions) { onScore(Team.YOU) }
        }

        // Drawing only; taps fall through to the zones underneath
        MatchBoard(
            display = display,
            names = names,
            modifier = Modifier.fillMaxSize().padding(
                start = hInset,
                end = hInset,
                top = scaledDp(M.TOP_INSET, min = M.TOP_INSET_MIN),
                bottom = scaledDp(M.BOTTOM_INSET),
            ),
        )

        UndoButton(
            onUndo = onUndo,
            onEndMatch = onEndMatch,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = scaledDp(M.UNDO_EDGE_PAD)).testTag(UiTags.UNDO),
        )

        display.prompt?.let { PromptView(it, onAnswer) }
    }
}

@Composable
private fun ColumnScope.TapZone(tag: String, label: String, actions: List<CustomAccessibilityAction>, onTap: () -> Unit) {
    Box(
        Modifier
            .weight(1f).fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClickLabel = label, role = Role.Button, onClick = onTap)
            .semantics {
                contentDescription = label
                customActions = actions
            }
            .testTag(tag),
    )
}

@Preview(device = WATCH_PREVIEW)
@Composable
private fun MatchScreenPreview() = WatchFrame {
    MatchScreen(SampleBoards.make(SampleBoards.MATCH)!!.match!!, TeamNames.DEFAULT, {}, {}, {}, {})
}
