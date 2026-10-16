package ch.wastedpotential.padel.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.wastedpotential.padel.board.BoardText
import ch.wastedpotential.padel.board.BoardWhite
import ch.wastedpotential.padel.board.LocalBoardScale
import ch.wastedpotential.padel.board.MatchBoard
import ch.wastedpotential.padel.board.SummaryHeader
import ch.wastedpotential.padel.board.SummaryResults
import ch.wastedpotential.padel.board.SummaryStatRows
import ch.wastedpotential.padel.board.accent
import ch.wastedpotential.padel.board.scaledDp
import ch.wastedpotential.padel.board.strokeIcon
import ch.wastedpotential.padel.display.BoardAlpha
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.PhoneBoard
import ch.wastedpotential.padel.sync.SampleBoards
import ch.wastedpotential.padel.sync.TeamNames
import ch.wastedpotential.padel.display.BoardMetrics as M

/** The smallest tap target on the phone (Android accessibility guidance). */
internal val MinTouchTarget: Dp = 48.dp

/** Courtside scoreboard: [BoardScreen] over the view model, plus the team-name editor. */
@Composable
fun BoardApp(vm: BoardViewModel) {
    val state = vm.state
    var editing by rememberSaveable { mutableStateOf(false) }
    if (state == null) {
        Box(Modifier.fillMaxSize().background(Color(BoardColors.BACKGROUND))) // the names are still opening
        return
    }
    BoardScreen(state, onEditNames = { editing = true })
    if (editing) {
        NameEditor(
            current = state.names,
            onDone = { you, opp ->
                vm.editNames(you, opp)
                editing = false
            },
            onCancel = { editing = false },
        )
    }
}

/** A big landscape mirror of the watch: its match, its summary, or the waiting board with the names. */
@Composable
fun BoardScreen(state: BoardState, onEditNames: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(BoardColors.BACKGROUND)).safeDrawingPadding()) {
        when {
            state.match != null -> CompositionLocalProvider(LocalBoardScale provides maxHeight.value / M.PHONE_MATCH_HEIGHT) {
                // Nothing on it is tappable, so TalkBack reads the whole board as one element
                MatchBoard(
                    state.match,
                    state.names,
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = maxWidth * M.PHONE_MATCH_INSET, vertical = maxHeight * M.PHONE_MATCH_INSET)
                        .semantics(mergeDescendants = true) { contentDescription = state.match.spokenScore(state.names) }
                        .testTag(UiTags.MATCH_BOARD),
                )
            }

            state.summary != null -> CompositionLocalProvider(LocalBoardScale provides maxHeight.value / M.PHONE_SUMMARY_HEIGHT) {
                PhoneSummary(
                    state.summary,
                    state.names,
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = maxWidth * M.PHONE_SUMMARY_INSET_H, vertical = maxHeight * M.PHONE_SUMMARY_INSET_V),
                )
            }

            else -> WaitingBoard(state.names, Modifier.align(Alignment.Center))
        }
        if (state.showsNameEditor) EditButton(Modifier.align(Alignment.TopEnd), onEditNames)
    }
}

/** The watch summary laid out for landscape: results on the left, statistics on the right. */
@Composable
private fun PhoneSummary(stats: SummaryStats, names: TeamNames, modifier: Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(scaledDp(M.PHONE_SUMMARY_GAP)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaledDp(M.PHONE_TITLE_GAP)),
        ) {
            SummaryHeader()
            SummaryResults(stats, names)
        }
        SummaryStatRows(stats, Modifier.width(scaledDp(M.PHONE_STATS_WIDTH)))
    }
}

/** Shows the names, so editing them gives feedback before a match starts. */
@Composable
private fun WaitingBoard(names: TeamNames, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = M.WAITING_PAD.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(M.WAITING_SPACING.dp),
    ) {
        Image(
            WatchIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Team.YOU.accent),
            modifier = Modifier.size(M.WAITING_ICON.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(M.WAITING_NAME_GAP.dp), verticalAlignment = Alignment.CenterVertically) {
            BoardText(
                names.name(Team.YOU),
                M.WAITING_NAMES,
                Team.YOU.accent,
                Modifier.weight(1f, fill = false),
                weight = FontWeight.Black,
                fit = true,
            )
            BoardText(UiText.VERSUS, M.WAITING_NAMES, BoardWhite.copy(alpha = BoardAlpha.DIM), weight = FontWeight.Black)
            BoardText(
                names.name(Team.OPP),
                M.WAITING_NAMES,
                Team.OPP.accent,
                Modifier.weight(1f, fill = false),
                weight = FontWeight.Black,
                fit = true,
            )
        }
        BoardText(UiText.WAITING, M.WAITING_TITLE, BoardWhite)
        BoardText(UiText.START_ON_WATCH, M.WAITING_HINT, BoardWhite.copy(alpha = BoardAlpha.HINT), weight = FontWeight.Normal)
    }
}

/** The pencil: a small icon in a full-size tap target. */
@Composable
private fun EditButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(M.EDIT_PAD.dp)
            .size(MinTouchTarget)
            .clickable(interactionSource = null, indication = null, onClickLabel = UiText.EDIT_NAMES, role = Role.Button, onClick = onClick)
            .testTag(UiTags.EDIT_NAMES),
    ) {
        Image(
            imageVector = PencilIcon,
            contentDescription = UiText.EDIT_NAMES,
            colorFilter = ColorFilter.tint(BoardWhite.copy(alpha = BoardAlpha.ICON)),
            modifier = Modifier.size(M.EDIT_ICON.dp),
        )
    }
}

private val PencilIcon = strokeIcon("Pencil") {
    moveTo(4f, 20f)
    lineTo(8f, 19f)
    lineTo(19f, 8f)
    lineTo(16f, 5f)
    lineTo(5f, 16f)
    close()
    moveTo(14f, 7f)
    lineTo(17f, 10f)
}

private val WatchIcon = strokeIcon("Watch") {
    moveTo(9f, 6f)
    lineTo(15f, 6f)
    arcToRelative(3f, 3f, 0f, false, true, 3f, 3f)
    lineTo(18f, 15f)
    arcToRelative(3f, 3f, 0f, false, true, -3f, 3f)
    lineTo(9f, 18f)
    arcToRelative(3f, 3f, 0f, false, true, -3f, -3f)
    lineTo(6f, 9f)
    arcToRelative(3f, 3f, 0f, false, true, 3f, -3f)
    close()
    moveTo(9f, 6f)
    lineTo(10f, 2f)
    lineTo(14f, 2f)
    lineTo(15f, 6f)
    moveTo(9f, 18f)
    lineTo(10f, 22f)
    lineTo(14f, 22f)
    lineTo(15f, 18f)
}

/** The board of a shared sample (or the waiting board for null), as the phone shows it. */
private fun previewState(sample: String?): BoardState {
    val board = PhoneBoard().apply { sample?.let { receive(SampleBoards.make(it)!!.encode()) } }
    return BoardState.of(board, TeamNames.DEFAULT)
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun MatchPreview() = BoardScreen(previewState(SampleBoards.MATCH)) {}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun SummaryPreview() = BoardScreen(previewState(SampleBoards.SUMMARY)) {}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun WaitingPreview() = BoardScreen(previewState(null)) {}
