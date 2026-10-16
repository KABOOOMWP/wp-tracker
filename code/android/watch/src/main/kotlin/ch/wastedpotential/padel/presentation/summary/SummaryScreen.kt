package ch.wastedpotential.padel.presentation.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ch.wastedpotential.padel.board.BoardText
import ch.wastedpotential.padel.board.BoardWhite
import ch.wastedpotential.padel.board.SummaryHeader
import ch.wastedpotential.padel.board.SummaryResults
import ch.wastedpotential.padel.board.SummaryStatRows
import ch.wastedpotential.padel.board.scaledDp
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.presentation.LocalIsRoundScreen
import ch.wastedpotential.padel.presentation.WATCH_PREVIEW
import ch.wastedpotential.padel.presentation.WatchFrame
import ch.wastedpotential.padel.sync.SampleBoards
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.launch
import ch.wastedpotential.padel.display.BoardMetrics as M

/** The shared summary parts in one scrolling column (crown or swipe), then reopen and new match. */
@Composable
fun SummaryScreen(stats: SummaryStats, names: TeamNames, onReopen: () -> Unit, onNewMatch: () -> Unit) {
    val hPad = scaledDp(if (LocalIsRoundScreen.current) M.SUMMARY_STATS_PAD_ROUND else M.SUMMARY_STATS_PAD)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onRotaryScrollEvent {
                scope.launch { scrollState.scrollBy(it.verticalScrollPixels) }
                true
            }
            .focusable()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SummaryHeader(Modifier.padding(top = scaledDp(M.SUMMARY_TITLE_TOP), bottom = scaledDp(M.SUMMARY_TITLE_BOTTOM)))
        SummaryResults(stats, names, Modifier.padding(horizontal = scaledDp(M.SUMMARY_RESULT_PAD)))
        SummaryStatRows(stats, Modifier.padding(horizontal = hPad).padding(top = scaledDp(M.STAT_GAP)))
        Spacer(Modifier.height(scaledDp(M.SUMMARY_BUTTONS_TOP)))
        SummaryButton(stats.reopenLabel, UiTags.REOPEN, onReopen)
        Spacer(Modifier.height(scaledDp(M.SUMMARY_BUTTON_GAP)))
        SummaryButton(UiText.NEW_MATCH, UiTags.NEW_MATCH, onNewMatch)
        Spacer(Modifier.height(scaledDp(M.SUMMARY_BOTTOM)))
    }
}

@Composable
private fun SummaryButton(label: String, tag: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = scaledDp(M.SUMMARY_BUTTON_PAD))
            .height(scaledDp(M.SUMMARY_BUTTON, min = M.SUMMARY_BUTTON_MIN))
            .background(Color(BoardColors.BUTTON), CircleShape)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .testTag(tag),
    ) {
        BoardText(label, M.BUTTON_FONT, BoardWhite, minSize = M.BUTTON_FONT_MIN)
    }
}

@Preview(device = WATCH_PREVIEW)
@Composable
private fun SummaryScreenPreview() = WatchFrame {
    SummaryScreen(SampleBoards.make(SampleBoards.SUMMARY)!!.summary!!, TeamNames.DEFAULT, {}, {})
}
