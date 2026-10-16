package ch.wastedpotential.padel.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.wastedpotential.padel.display.BoardAlpha
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.StatRow
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.TeamNames
import ch.wastedpotential.padel.display.BoardMetrics as M

/** "MATCH SUMMARY" above the result panels. */
@Composable
fun SummaryHeader(modifier: Modifier = Modifier) {
    BoardText(
        UiText.MATCH_SUMMARY,
        M.SUMMARY_HEADER,
        BoardWhite.copy(alpha = BoardAlpha.TITLE),
        modifier,
        minSize = M.SUMMARY_HEADER_MIN,
        weight = FontWeight.Medium,
        spacing = 2f,
    )
}

/** Opponent result, the result divider, your result. */
@Composable
fun SummaryResults(stats: SummaryStats, names: TeamNames, modifier: Modifier = Modifier) {
    Column(modifier) {
        SummaryTeamPanel(Team.OPP, names.summaryName(Team.OPP), stats)
        PillDivider(stats.resultLabel, Modifier.height(scaledDp(M.DIVIDER)), pillColor = stats.winner?.accent ?: Color(BoardColors.PILL))
        SummaryTeamPanel(Team.YOU, names.summaryName(Team.YOU), stats)
    }
}

/** A team's result: name, games per set, and sets won; read out as one item. */
@Composable
private fun SummaryTeamPanel(team: Team, name: String, stats: SummaryStats) {
    val accent = team.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(scaledDp(M.SUMMARY_PANEL))
            .background(team.panel, RoundedCornerShape(scaledDp(M.SUMMARY_CORNER)))
            .padding(start = scaledDp(M.SUMMARY_PAD_START), end = scaledDp(M.SUMMARY_PAD_END))
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(scaledDp(M.SUMMARY_LINE_GAP))) {
            BoardText(name, M.SUMMARY_LABEL, accent, fit = true)
            Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(M.SET_SPACING))) {
                stats.sets.forEach { set ->
                    val lost = set.lostBy(team)
                    BoardText(
                        text = set.games(team).toString(),
                        size = M.SUMMARY_SETS,
                        color = accent.copy(alpha = if (lost) BoardAlpha.DIM else 1f),
                        weight = if (set.winner == team) FontWeight.Bold else FontWeight.Normal,
                        italic = lost,
                    )
                }
            }
        }
        BoardText(stats.setsWon(team).toString(), M.SUMMARY_WON, accent)
    }
}

/** YOU – OPP header, the split stats, a divider, then the single-value stats. */
@Composable
fun SummaryStatRows(stats: SummaryStats, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(scaledDp(M.STAT_GAP))) {
        Row(Modifier.fillMaxWidth().padding(top = scaledDp(M.STAT_HEADER_PAD)), horizontalArrangement = Arrangement.End) {
            StatHeader(UiText.YOU_SHORT, Color(BoardColors.YOU_ACCENT))
            StatHeader(UiText.STATS_SEPARATOR, BoardWhite)
            StatHeader(UiText.OPP_SHORT, Color(BoardColors.OPP_ACCENT))
        }
        stats.splitRows.forEach { StatLine(it) }
        Box(
            Modifier.fillMaxWidth().padding(
                vertical = scaledDp(M.STAT_RULE_PAD),
            ).height(1.dp).background(BoardWhite.copy(alpha = BoardAlpha.RULE)),
        )
        stats.detailRows.forEach { StatLine(it) }
    }
}

@Composable
private fun StatHeader(text: String, color: Color) =
    BoardText(text, M.STAT_HEADER_FONT, color, minSize = M.STAT_HEADER_FONT_MIN, weight = FontWeight.Medium)

@Composable
private fun StatLine(row: StatRow) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        BoardText(row.label, M.STAT_FONT, BoardWhite.copy(alpha = BoardAlpha.LABEL), minSize = M.STAT_FONT_MIN, weight = FontWeight.Normal)
        BoardText(row.value, M.STAT_FONT, BoardWhite, minSize = M.STAT_FONT_MIN, weight = FontWeight.Medium)
    }
}
