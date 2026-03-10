package com.wptracker.presentation.setup

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.wptracker.model.*
import com.wptracker.presentation.theme.LocalWatchScale
import com.wptracker.presentation.theme.WPColors

private sealed class SetupStep {
    object PlayMode    : SetupStep()
    object MatchFormat : SetupStep()
    object RuleMode    : SetupStep()
    object WhoServes   : SetupStep()
    object ServeOrder  : SetupStep()
}

@Composable
fun SetupScreen(onMatchStart: (Config) -> Unit) {
    val scale    = LocalWatchScale.current
    val labelSz  = (24f * scale).sp
    val view     = LocalView.current

    var step         by remember { mutableStateOf<SetupStep>(SetupStep.PlayMode) }
    var playMode     by remember { mutableStateOf<PlayMode?>(null) }
    var bestOf       by remember { mutableStateOf<Int?>(null) }
    var ruleMode     by remember { mutableStateOf<RuleMode?>(null) }
    var startingTeam by remember { mutableStateOf<Team?>(null) }
    val serveOrderSelection = remember { mutableStateListOf<Player>() }

    when (step) {
        // ── Play Mode ─────────────────────────────────────────────────────
        SetupStep.PlayMode -> Box(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            playMode = PlayMode.SINGLES; step = SetupStep.MatchFormat
                        }
                    }
            ) { Text("1 vs 1", color = WPColors.OppAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            playMode = PlayMode.DOUBLES; step = SetupStep.MatchFormat
                        }
                    }
            ) { Text("2 vs 2", color = WPColors.YouAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
            SetupPill("PLAY MODE", Modifier.align(Alignment.Center))
        }

        // ── Match Format ──────────────────────────────────────────────────
        SetupStep.MatchFormat -> Box(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            bestOf = 3; step = SetupStep.RuleMode
                        }
                    }
            ) { Text("Best of 3", color = WPColors.OppAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            bestOf = 5; step = SetupStep.RuleMode
                        }
                    }
            ) { Text("Best of 5", color = WPColors.YouAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
            SetupPill("FORMAT", Modifier.align(Alignment.Center))
        }

        // ── Rule Mode ─────────────────────────────────────────────────────
        SetupStep.RuleMode -> Column(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f).background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            ruleMode = RuleMode.STANDARD
                            if (playMode == PlayMode.DOUBLES) { serveOrderSelection.clear(); startingTeam = null; step = SetupStep.ServeOrder }
                            else step = SetupStep.WhoServes
                        }
                    }
            ) { Text("Standard", color = WPColors.OppAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
                SetupPill("GAME", Modifier.align(Alignment.Center))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF1E1E1E))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            ruleMode = RuleMode.GOLDEN
                            if (playMode == PlayMode.DOUBLES) { serveOrderSelection.clear(); startingTeam = null; step = SetupStep.ServeOrder }
                            else step = SetupStep.WhoServes
                        }
                    }
            ) { Text("Golden Point", color = Color.White, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
                SetupPill("MODE", Modifier.align(Alignment.Center))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f).background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            ruleMode = RuleMode.STAR
                            if (playMode == PlayMode.DOUBLES) { serveOrderSelection.clear(); startingTeam = null; step = SetupStep.ServeOrder }
                            else step = SetupStep.WhoServes
                        }
                    }
            ) { Text("Star Point", color = WPColors.YouAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
        }

        // ── Who Serves ────────────────────────────────────────────────────
        SetupStep.WhoServes -> Box(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            startingTeam = Team.OPP
                            if (playMode == PlayMode.SINGLES) finish(playMode!!, bestOf!!, ruleMode!!, listOf(Player.B1, Player.A1), onMatchStart)
                            else { serveOrderSelection.clear(); step = SetupStep.ServeOrder }
                        }
                    }
            ) { Text("OPPONENT", color = WPColors.OppAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            startingTeam = Team.YOU
                            if (playMode == PlayMode.SINGLES) finish(playMode!!, bestOf!!, ruleMode!!, listOf(Player.A1, Player.B1), onMatchStart)
                            else { serveOrderSelection.clear(); step = SetupStep.ServeOrder }
                        }
                    }
            ) { Text("YOU", color = WPColors.YouAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
            SetupPill("WHO SERVES?", Modifier.align(Alignment.Center))
        }

        // ── Order of Serve ────────────────────────────────────────────────
        SetupStep.ServeOrder -> Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    ServeOrderCell("OPP L", Player.B2, serveOrderSelection) { onServeOrderTap(view, serveOrderSelection, Player.B2) }
                    ServeOrderCell("OPP R", Player.B1, serveOrderSelection) { onServeOrderTap(view, serveOrderSelection, Player.B1) }
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    ServeOrderCell("YOU L", Player.A2, serveOrderSelection) { onServeOrderTap(view, serveOrderSelection, Player.A2) }
                    ServeOrderCell("YOU R", Player.A1, serveOrderSelection) { onServeOrderTap(view, serveOrderSelection, Player.A1) }
                }
            }
            Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White).align(Alignment.Center))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
            SetupPill("ORDER OF SERVE", Modifier.align(Alignment.Center))
        }
    }

    LaunchedEffect(serveOrderSelection.size, step) {
        if (step == SetupStep.ServeOrder && serveOrderSelection.size == 4) {
            finish(playMode!!, bestOf!!, ruleMode!!, serveOrderSelection.toList(), onMatchStart)
        }
    }
}

private fun finish(
    playMode: PlayMode, bestOf: Int, ruleMode: RuleMode,
    order: List<Player>, onMatchStart: (Config) -> Unit
) {
    onMatchStart(Config(bestOf = bestOf, ruleMode = ruleMode, playMode = playMode, serveOrder = order))
}

private fun onServeOrderTap(
    view: android.view.View,
    order: MutableList<Player>,
    player: Player
) {
    // Tapping an already-selected player resets the entire selection.
    // Partial removal would shift indices and could violate the alternating-team rule.
    if (order.contains(player)) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        order.clear()
        return
    }
    val idx = order.size
    if (idx >= 4) return
    val firstTeam    = if (order.isEmpty()) teamOf(player) else teamOf(order[0])
    val expectedTeam = if (idx % 2 == 0) firstTeam else oppositeTeam(firstTeam)
    if (teamOf(player) != expectedTeam) return
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    order.add(player)
}

private fun teamOf(player: Player): Team =
    if (player == Player.A1 || player == Player.A2) Team.YOU else Team.OPP

private fun oppositeTeam(team: Team): Team =
    if (team == Team.YOU) Team.OPP else Team.YOU

@Composable
private fun RowScope.ServeOrderCell(
    title: String,
    player: Player,
    selectedOrder: List<Player>,
    onTap: () -> Unit
) {
    val scale     = LocalWatchScale.current
    val titleSz   = (12f * scale).sp
    val numberSz  = (22f * scale).sp
    val idx       = selectedOrder.indexOf(player)
    val active    = idx == -1
    val inYouTeam = player == Player.A1 || player == Player.A2
    val bg        = if (inYouTeam) WPColors.YouPanel else WPColors.OppPanel
    val fg        = if (inYouTeam) WPColors.YouAccent else WPColors.OppAccent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f).fillMaxHeight()
            .background(bg.copy(alpha = if (active) 1f else 0.35f))
            .pointerInput(player, selectedOrder.size) { detectTapGestures { onTap() } }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, color = fg, fontSize = titleSz, fontWeight = FontWeight.Bold)
            if (idx != -1) {
                Text((idx + 1).toString(), color = Color.White, fontSize = numberSz, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetupPill(text: String, modifier: Modifier = Modifier) {
    val scale = LocalWatchScale.current
    val pillSz = (9f * scale).coerceAtLeast(7f).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(WPColors.PillBg, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text          = text,
            color         = Color.Black,
            fontSize      = pillSz,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
