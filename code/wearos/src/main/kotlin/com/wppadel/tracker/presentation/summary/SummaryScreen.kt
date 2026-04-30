package com.wppadel.tracker.presentation.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.wppadel.tracker.model.RuleMode
import com.wppadel.tracker.model.Snapshot
import com.wppadel.tracker.presentation.theme.LocalIsRoundScreen
import com.wppadel.tracker.presentation.theme.LocalWatchScale
import com.wppadel.tracker.presentation.theme.WPColors
import kotlin.math.abs

@Composable
fun SummaryScreen(
    snapshot   : Snapshot,
    onNewMatch : () -> Unit
) {
    val scale          = LocalWatchScale.current
    val isRound        = LocalIsRoundScreen.current
    val scrollState    = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { try { focusRequester.requestFocus() } catch (_: IllegalStateException) {} }
    val match    = snapshot.match
    val youWon   = match.setsWonYou > match.setsWonOpp
    val duration = formatDuration(match.startedAt, match.endedAt ?: System.currentTimeMillis())
    val sets     = match.setScores.filter { it.isCompleted }
    val deciders = snapshot.stats.goldenDecidersPlayed

    val totalYouGames = sets.sumOf { it.youGames.toInt() }
    val totalOppGames = sets.sumOf { it.oppGames.toInt() }
    val totalGames    = totalYouGames + totalOppGames
    val tiebreaks     = sets.count { it.youGames == 7 || it.oppGames == 7 }
    val avgPtsPerGame = if (totalGames > 0) snapshot.stats.totalPlayedPoints.toInt() / totalGames else 0
    val pointsOpp     = snapshot.stats.totalPlayedPoints.toInt() - snapshot.stats.pointsWonYou.toInt()

    // Scale-derived sizes
    val panelH      = (54f * scale).dp
    val setsWonSz   = (30f * scale).sp
    val setScoreSz  = (12f * scale).sp
    val teamLblSz   = (9f  * scale).sp
    val headerSz    = (8f  * scale).coerceAtLeast(7f).sp
    val dividerH    = (20f * scale).dp
    val hPad        = ((if (isRound) 22f else 12f) * scale).dp
    val trailPad    = (14f * scale).dp
    val colHdrSz    = (7f  * scale).coerceAtLeast(6f).sp
    val btnSz       = (11f * scale).coerceAtLeast(9f).sp

    Box(modifier = Modifier.fillMaxSize().background(WPColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onRotaryScrollEvent { event ->
                    coroutineScope.launch { scrollState.scrollBy(event.verticalScrollPixels) }
                    true
                }
                .focusable()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────
            Text(
                text          = "MATCH SUMMARY",
                color         = Color.White.copy(alpha = 0.4f),
                fontSize      = headerSz,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 2.sp,
                modifier      = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // ── Opponent panel ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelH)
                    .padding(horizontal = 10.dp)
                    .background(WPColors.OppPanel, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier          = Modifier.fillMaxSize().padding(start = hPad, end = trailPad),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("OPPONENT", color = WPColors.OppAccent,
                            fontSize = teamLblSz, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sets.forEach { s ->
                                val won = s.oppGames > s.youGames
                                Text(
                                    text       = s.oppGames.toString(),
                                    color      = WPColors.OppAccent.copy(alpha = if (won) 1f else 0.5f),
                                    fontSize   = setScoreSz,
                                    fontWeight = if (won) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle  = if (!won) FontStyle.Italic else FontStyle.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(match.setsWonOpp.toString(), color = WPColors.OppAccent,
                        fontSize = setsWonSz, fontWeight = FontWeight.Bold)
                }
            }

            // ── Divider with winner pill ──────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxWidth().height(dividerH),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(WPColors.Divider))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            if (youWon) WPColors.YouAccent else WPColors.OppAccent,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text          = if (youWon) "YOU WIN" else "OPPONENT WINS",
                        color         = Color.Black,
                        fontSize      = headerSz,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // ── You panel ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelH)
                    .padding(horizontal = 10.dp)
                    .background(WPColors.YouPanel, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier          = Modifier.fillMaxSize().padding(start = hPad, end = trailPad),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("YOU", color = WPColors.YouAccent,
                            fontSize = teamLblSz, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sets.forEach { s ->
                                val won = s.youGames > s.oppGames
                                Text(
                                    text       = s.youGames.toString(),
                                    color      = WPColors.YouAccent.copy(alpha = if (won) 1f else 0.5f),
                                    fontSize   = setScoreSz,
                                    fontWeight = if (won) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle  = if (!won) FontStyle.Italic else FontStyle.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(match.setsWonYou.toString(), color = WPColors.YouAccent,
                        fontSize = setsWonSz, fontWeight = FontWeight.Bold)
                }
            }

            // ── YOU – OPP column header ───────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = hPad).padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text("YOU",   color = WPColors.YouAccent, fontSize = colHdrSz, fontWeight = FontWeight.Medium)
                Text("  –  ", color = Color.White,        fontSize = colHdrSz, fontWeight = FontWeight.Medium)
                Text("OPP",   color = WPColors.OppAccent, fontSize = colHdrSz, fontWeight = FontWeight.Medium)
            }

            // ── Group 1: Split stats ───────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = hPad).padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatRow("Points", "${snapshot.stats.pointsWonYou} – $pointsOpp", scale)
                StatRow("Games",  "$totalYouGames – $totalOppGames",              scale)
                StatRow("Breaks", "${snapshot.stats.breaksYou} – ${snapshot.stats.breaksOpp}", scale)
            }

            // ── Subtle divider ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad)
                    .padding(vertical = 6.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // ── Group 2: Single-value stats ───────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = hPad),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatRow("Duration",     duration,           scale)
                StatRow("Avg pts/game", avgPtsPerGame.toString(), scale)
                if (tiebreaks > 0) {
                    StatRow("Tiebreaks", tiebreaks.toString(), scale)
                }
                if (snapshot.stats.deuceCount > 0) {
                    StatRow("Deuces", snapshot.stats.deuceCount.toString(), scale)
                }
                if (snapshot.config.ruleMode != RuleMode.STANDARD && deciders > 0) {
                    val label = if (snapshot.config.ruleMode == RuleMode.GOLDEN) "Golden won" else "Star won"
                    StatRow(label, "${snapshot.stats.goldenDecidersWonYou} / $deciders", scale)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── New match button ───────────────────────────────────────────
            Chip(
                onClick  = onNewMatch,
                label    = {
                    Text("NEW MATCH", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = btnSz,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                colors   = ChipDefaults.chipColors(backgroundColor = Color(0xFF242424))
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, scale: Float) {
    val sz = (9f * scale).coerceAtLeast(7f).sp
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = sz)
        Text(value, color = Color.White, fontSize = sz, fontWeight = FontWeight.Medium)
    }
}

private fun formatDuration(startMs: Long, endMs: Long): String {
    val totalMin = abs(endMs - startMs) / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return "%d:%02d".format(h, m)
}
