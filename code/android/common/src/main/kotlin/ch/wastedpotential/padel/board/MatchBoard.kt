package ch.wastedpotential.padel.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.TeamNames
import ch.wastedpotential.padel.display.BoardMetrics as M

/** Opponent panel, set scores with the status pill, your panel: the match screen without its controls. */
@Composable
fun MatchBoard(display: MatchDisplay, names: TeamNames, modifier: Modifier = Modifier) {
    Column(modifier) {
        TeamPanel(Team.OPP, names.name(Team.OPP), display, Modifier.weight(1f).fillMaxWidth())
        MidRow(display, Modifier.fillMaxWidth().height(scaledDp(M.MID_ROW)))
        TeamPanel(Team.YOU, names.name(Team.YOU), display, Modifier.weight(1f).fillMaxWidth())
    }
}

/** The status pill of the match and setup screens (golden), and of the summary result (the winner's colour). */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Color(BoardColors.PILL)) {
    BoardText(
        text = text,
        size = M.PILL_FONT,
        minSize = M.PILL_FONT_MIN,
        color = Color(BoardColors.BACKGROUND),
        spacing = 0.8f,
        modifier = modifier
            .background(color, CircleShape)
            .padding(horizontal = scaledDp(M.PILL_PAD_H), vertical = scaledDp(M.PILL_PAD_V)),
    )
}

/** A full-width white line with an optional [Pill] on it. */
@Composable
fun PillDivider(label: String?, modifier: Modifier = Modifier, pillColor: Color = Color(BoardColors.PILL)) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(BoardWhite))
        if (label != null) Pill(label, color = pillColor)
    }
}

@Composable
private fun TeamPanel(team: Team, name: String, display: MatchDisplay, modifier: Modifier) {
    val onLeft = display.scoreOnLeft(team)
    val centered = display.scoresCentered
    val corner = scaledDp(M.CORNER)
    val labelPad = scaledDp(M.LABEL_PAD)
    val scorePad = scaledDp(M.SCORE_PAD)

    Box(modifier.clip(RoundedCornerShape(corner)).background(team.panel)) {
        BoardText(
            text = name,
            size = M.LABEL_FONT,
            color = team.accent,
            fit = true,
            modifier = Modifier.align(Alignment.TopStart).padding(start = labelPad, end = labelPad, top = labelPad),
        )

        if (display.servingTeam == team) ServeStripe(display.serveStripeOnLeft, team.accent, corner)

        BoardText(
            text = display.score(team),
            size = M.SCORE_FONT,
            color = BoardWhite,
            modifier = Modifier
                .align(
                    when {
                        centered -> Alignment.BottomCenter
                        onLeft -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    },
                )
                .padding(
                    start = if (!centered && onLeft) scorePad else 0.dp,
                    end = if (!centered && !onLeft) scorePad else 0.dp,
                    bottom = scorePad,
                )
                .testTag(if (team == Team.YOU) UiTags.SCORE_YOU else UiTags.SCORE_OPP),
        )
    }
}

/** Thin bar on the serving player's court side. */
@Composable
private fun BoxScope.ServeStripe(onLeft: Boolean, accent: Color, corner: Dp) {
    val pad = scaledDp(M.STRIPE_PAD, min = M.STRIPE_PAD_MIN)
    val width = scaledDp(M.STRIPE_WIDTH, min = M.STRIPE_WIDTH_MIN)
    Box(
        Modifier
            .align(if (onLeft) Alignment.CenterStart else Alignment.CenterEnd)
            .padding(vertical = corner + M.STRIPE_V_PAD.dp)
            .padding(start = if (onLeft) pad else 0.dp, end = if (onLeft) 0.dp else pad)
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(width / 2))
            .background(accent)
            .testTag(UiTags.SERVE_STRIPE),
    )
}

@Composable
private fun MidRow(display: MatchDisplay, modifier: Modifier) {
    Box(modifier) {
        PillDivider(null, Modifier.align(Alignment.Center))
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = scaledDp(M.CORNER)),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            SetGamesRow(display.setScores, Team.OPP)
            SetGamesRow(display.setScores, Team.YOU)
        }
        display.pillLabel?.let { Pill(it, Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun SetGamesRow(sets: List<SetScore>, team: Team) {
    Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(M.SET_SPACING))) {
        sets.forEach { set ->
            BoardText(
                text = set.games(team).toString(),
                size = M.SET_FONT,
                minSize = M.SET_FONT_MIN,
                color = team.accent,
                weight = if (set.winner == team) FontWeight.Bold else FontWeight.Normal,
                italic = set.lostBy(team),
            )
        }
    }
}
