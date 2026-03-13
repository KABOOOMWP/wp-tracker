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
    object WhichPlayer : SetupStep()
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
                            step = SetupStep.WhoServes
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
                            step = SetupStep.WhoServes
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
                            step = SetupStep.WhoServes
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
                            else step = SetupStep.WhichPlayer
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
                            else step = SetupStep.WhichPlayer
                        }
                    }
            ) { Text("YOU", color = WPColors.YouAccent, fontSize = labelSz, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.Center))
            SetupPill("WHO SERVES?", Modifier.align(Alignment.Center))
        }

        // ── Which Player (doubles only) ───────────────────────────────────
        SetupStep.WhichPlayer -> WhichPlayerScreen(
            scale        = scale,
            view         = view,
            startingTeam = startingTeam,
            onLeft = {
                val p   = if (startingTeam == Team.YOU) Player.A2 else Player.B2
                val opp = if (startingTeam == Team.YOU) Player.B1 else Player.A1
                finish(playMode!!, bestOf!!, ruleMode!!, listOf(p, opp, partnerOf(p), partnerOf(opp)), onMatchStart)
            },
            onRight = {
                val p   = if (startingTeam == Team.YOU) Player.A1 else Player.B1
                val opp = if (startingTeam == Team.YOU) Player.B1 else Player.A1
                finish(playMode!!, bestOf!!, ruleMode!!, listOf(p, opp, partnerOf(p), partnerOf(opp)), onMatchStart)
            }
        )
    }
}

private fun finish(
    playMode: PlayMode, bestOf: Int, ruleMode: RuleMode,
    order: List<Player>, onMatchStart: (Config) -> Unit
) {
    onMatchStart(Config(bestOf = bestOf, ruleMode = ruleMode, playMode = playMode, serveOrder = order))
}

private fun partnerOf(player: Player): Player = when (player) {
    Player.A1 -> Player.A2
    Player.A2 -> Player.A1
    Player.B1 -> Player.B2
    Player.B2 -> Player.B1
}

@Composable
private fun WhichPlayerScreen(
    scale        : Float,
    view         : android.view.View,
    startingTeam : com.wptracker.model.Team?,
    onLeft       : () -> Unit,
    onRight      : () -> Unit
) {
    val titleFont = (11f * scale).coerceAtLeast(9f).sp
    val subFont   = (8f  * scale).coerceAtLeast(7f).sp
    val btnFont   = (14f * scale).coerceAtLeast(11f).sp

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onLeft()
                        }
                    }
            ) { Text("← LEFT", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onRight()
                        }
                    }
            ) { Text("RIGHT →", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold) }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(top = 6.dp, start = 8.dp, end = 8.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text          = "WHO SERVES?",
                color         = Color.White.copy(alpha = 0.9f),
                fontSize      = titleFont,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text     = if (startingTeam == com.wptracker.model.Team.YOU) "YOUR SIDE" else "OPPONENT SIDE",
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = subFont
            )
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
