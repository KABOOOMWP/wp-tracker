package com.wptracker.presentation.match

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.wptracker.engine.MatchEngine
import com.wptracker.haptic.HapticManager
import com.wptracker.model.*
import com.wptracker.presentation.theme.LocalIsRoundScreen
import com.wptracker.presentation.theme.LocalWatchScale
import com.wptracker.presentation.theme.WPColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Layout constants scaled from the 192 dp reference (see Theme.kt).
// `round` increases the horizontal inset to keep content away from the curved edges.
private data class ML(val s: Float, val round: Boolean) {
    val hInset      = ((if (round) 22f else 10f) * s).dp
    val topVInset   = (2f  * s).coerceAtLeast(1f).dp
    val botVInset   = (4f  * s).dp
    val midRowH     = (26f * s).dp
    val corner      = (18f * s).dp
    val labelPad    = (14f * s).dp
    val scorePad    = (14f * s).dp
    val stripePad   = (3f  * s).coerceAtLeast(2f).dp
    val scoreFont   = (36f * s).sp
    val labelFont   = (16f * s).sp
    val pillFont    = (9f  * s).coerceAtLeast(7f).sp
    val setFont     = (10f * s).coerceAtLeast(8f).sp
    val scorePadBot = (14f * s).dp
}

@Composable
fun MatchScreen(
    config    : Config,
    haptic    : HapticManager,
    onMatchEnd: (Snapshot) -> Unit,
    vm        : MatchViewModel = viewModel()
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(config) { vm.init(config) }
    val snapshot = vm.current ?: return
    val scale   = LocalWatchScale.current
    val isRound = LocalIsRoundScreen.current
    val ml = remember(scale, isRound) { ML(scale, isRound) }

    val pill = MatchEngine.computePill(snapshot)
    val awaitingServePick = snapshot.awaitingServePick
    val serverTeam = snapshot.serve.serverTeam
    val serveLeft  = snapshot.serve.serveSide == ServeSide.LEFT
    // A2/B2 are left-side players; A1/B1 are right-side players.
    val serverIsSecondPlayer =
        snapshot.serve.serverPlayer == Player.A2 || snapshot.serve.serverPlayer == Player.B2
    val stripeLeft = serverIsSecondPlayer
    val oppLeft    = !serveLeft
    val youLeft    = serveLeft

    LaunchedEffect(snapshot.isMatchOver) {
        if (snapshot.isMatchOver) {
            delay(400)
            onMatchEnd(snapshot.copy(match = snapshot.match.copy(endedAt = System.currentTimeMillis())))
        }
    }

    val needsDeciderPick =
        (snapshot.game.phase == GamePhase.GOLDEN || snapshot.game.phase == GamePhase.STAR_POINT) &&
            snapshot.game.deciderReceiveSideOverride == null

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Full-screen tap zones ─────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            TapZone(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val prev = snapshot
                vm.score(Team.OPP)
                val next = vm.current ?: return@TapZone
                if (next !== prev) {
                    if (wasGameWon(prev, next)) haptic.gameWin() else haptic.pointOpp()
                }
            }
            TapZone(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val prev = snapshot
                vm.score(Team.YOU)
                val next = vm.current ?: return@TapZone
                if (next !== prev) {
                    if (wasGameWon(prev, next)) haptic.gameWin() else haptic.pointYou()
                }
            }
        }

        // ── Main visual layout (no hit-testing) ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = ml.hInset, end = ml.hInset,
                         top = ml.topVInset, bottom = ml.botVInset)
        ) {
            TeamPanel(
                label = "Opponent",
                labelColor = WPColors.OppAccent,
                panelColor = WPColors.OppPanel,
                score = formatScore(snapshot, Team.OPP),
                leftSide = oppLeft,
                stripeLeftSide = stripeLeft,
                showServeStripe = serverTeam == Team.OPP,
                stripeColor = WPColors.OppAccent,
                ml = ml,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            MidRow(
                setScores = displayedSetScores(snapshot),
                pill = pill,
                ml = ml,
                modifier = Modifier.fillMaxWidth().height(ml.midRowH)
            )

            TeamPanel(
                label = "You",
                labelColor = WPColors.YouAccent,
                panelColor = WPColors.YouPanel,
                score = formatScore(snapshot, Team.YOU),
                leftSide = youLeft,
                stripeLeftSide = stripeLeft,
                showServeStripe = serverTeam == Team.YOU,
                stripeColor = WPColors.YouAccent,
                ml = ml,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }

        // ── Undo button ───────────────────────────────────────────────────
        UndoButton(
            onUndo     = { vm.undo(); haptic.undo() },
            onEndMatch = { onMatchEnd(snapshot.copy(match = snapshot.match.copy(endedAt = System.currentTimeMillis()))) },
            modifier   = Modifier.align(Alignment.CenterEnd).padding(end = (6f * scale).dp)
        )

        // ── Decider-side picker ───────────────────────────────────────────
        if (needsDeciderPick) {
            DeciderSidePicker(
                receivingTeam = if (serverTeam == Team.YOU) Team.OPP else Team.YOU,
                onPick = { side -> vm.setDeciderSide(side) }
            )
        }

        // ── Serve-pick overlay (doubles game 2) ───────────────────────────
        if (awaitingServePick) {
            ServePickOverlay(servingTeam = serverTeam) { player ->
                vm.pickOpponentFirstServer(player)
            }
        }
    }
}

@Composable
private fun TapZone(modifier: Modifier, onTap: () -> Unit) {
    // rememberUpdatedState ensures the pointerInput coroutine (which never restarts because
    // its key is Unit) always calls the LATEST lambda — i.e. the one with the current
    // snapshot captured at the last recomposition. Without this, `prev` inside onTap would
    // be stale after a picker overlay changes state, making wasGameWon() return wrong results
    // and firing the wrong haptic pattern.
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) }
    )
}

@Composable
private fun TeamPanel(
    label: String,
    labelColor: Color,
    panelColor: Color,
    score: String,
    leftSide: Boolean,
    stripeLeftSide: Boolean,
    showServeStripe: Boolean,
    stripeColor: Color,
    ml: ML,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ml.corner))
            .background(panelColor)
    ) {
        Text(
            text       = label,
            color      = labelColor,
            fontSize   = ml.labelFont,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier
                .align(Alignment.TopStart)
                .padding(start = ml.labelPad, top = ml.labelPad)
        )

        if (showServeStripe) {
            ServeStripe(leftSide = stripeLeftSide, accent = stripeColor, ml = ml)
        }

        Text(
            text       = score,
            color      = Color.White,
            fontSize   = ml.scoreFont,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier
                .align(if (leftSide) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(start = if (leftSide) ml.scorePad else 0.dp)
                .padding(end   = if (leftSide) 0.dp else ml.scorePad)
                .padding(bottom = ml.scorePadBot)
        )
    }
}

@Composable
private fun BoxScope.ServeStripe(leftSide: Boolean, accent: Color, ml: ML) {
    Box(
        modifier = Modifier
            .align(if (leftSide) Alignment.CenterStart else Alignment.CenterEnd)
            .padding(top    = ml.corner + 2.dp, bottom = ml.corner + 2.dp)
            .padding(start  = if (leftSide) ml.stripePad else 0.dp)
            .padding(end    = if (leftSide) 0.dp else ml.stripePad)
            .width(3.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(1.5.dp))
            .background(accent)
    )
}

@Composable
private fun MidRow(setScores: List<SetScore>, pill: PillState, ml: ML, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White)
                .align(Alignment.Center)
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = ml.corner),
            horizontalArrangement = Arrangement.Start
        ) {
            SetGamesByDivider(setScores, ml)
        }

        if (pill != PillState.HIDDEN) {
            StatusPill(pill = pill, ml = ml, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SetGamesByDivider(setScores: List<SetScore>, ml: ML) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            setScores.forEach { set -> SetScoreDigit(value = set.oppGames, team = Team.OPP, set = set, ml = ml) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            setScores.forEach { set -> SetScoreDigit(value = set.youGames, team = Team.YOU, set = set, ml = ml) }
        }
    }
}

@Composable
private fun SetScoreDigit(value: Int, team: Team, set: SetScore, ml: ML) {
    val youWon      = set.youGames > set.oppGames
    val thisTeamWon = (team == Team.YOU && youWon) || (team == Team.OPP && !youWon)
    val thisTeamLost = set.isCompleted && !thisTeamWon
    Text(
        text       = value.toString(),
        color      = if (team == Team.YOU) WPColors.YouAccent else WPColors.OppAccent,
        fontSize   = ml.setFont,
        fontWeight = if (set.isCompleted && thisTeamWon) FontWeight.Bold else FontWeight.Normal,
        fontStyle  = if (thisTeamLost) FontStyle.Italic else FontStyle.Normal
    )
}

@Composable
private fun StatusPill(pill: PillState, ml: ML, modifier: Modifier) {
    val label = when (pill) {
        PillState.MATCH_POINT  -> "MATCH POINT"
        PillState.SET_POINT    -> "SET POINT"
        PillState.BREAK_POINT  -> "BREAK POINT"
        PillState.GOLDEN_POINT -> "GOLDEN POINT"
        PillState.STAR_POINT   -> "STAR POINT"
        PillState.TIEBREAK     -> "TIE-BREAK"
        PillState.HIDDEN       -> return
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(WPColors.PillBg, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text          = label,
            color         = WPColors.PillText,
            fontSize      = ml.pillFont,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun BoxScope.DeciderSidePicker(
    receivingTeam: Team,
    onPick: (ServeSide) -> Unit
) {
    val scale     = LocalWatchScale.current
    val titleFont = (11f * scale).coerceAtLeast(9f).sp
    val subFont   = (8f  * scale).coerceAtLeast(7f).sp
    val btnFont   = (14f * scale).coerceAtLeast(11f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
    ) {
        // Full-height tap buttons behind everything
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) { detectTapGestures { onPick(ServeSide.LEFT) } }
            ) {
                Text("← LEFT", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) { detectTapGestures { onPick(ServeSide.RIGHT) } }
            ) {
                Text("RIGHT →", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold)
            }
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
                text          = "SERVE FROM\nWHICH SIDE?",
                color         = Color.White.copy(alpha = 0.9f),
                fontSize      = titleFont,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign     = TextAlign.Center
            )
            Text(
                text     = "${if (receivingTeam == Team.YOU) "YOU" else "OPP"} RECEIVE",
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = subFont
            )
        }
    }
}

@Composable
private fun BoxScope.ServePickOverlay(
    servingTeam: Team,
    onPick: (Player) -> Unit
) {
    val scale     = LocalWatchScale.current
    val titleFont = (11f * scale).coerceAtLeast(9f).sp
    val subFont   = (8f  * scale).coerceAtLeast(7f).sp
    val btnFont   = (14f * scale).coerceAtLeast(11f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.YouPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onPick(if (servingTeam == Team.YOU) Player.A2 else Player.B2)
                        }
                    }
            ) {
                Text("← LEFT", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(WPColors.OppPanel)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onPick(if (servingTeam == Team.YOU) Player.A1 else Player.B1)
                        }
                    }
            ) {
                Text("RIGHT →", color = Color.White, fontSize = btnFont, fontWeight = FontWeight.Bold)
            }
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
                letterSpacing = 0.5.sp,
                textAlign     = TextAlign.Center
            )
            Text(
                text     = if (servingTeam == Team.YOU) "YOUR SIDE" else "OPPONENT SIDE",
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = subFont
            )
        }
    }
}

private enum class UndoBtnState { ARROW, X }

@Composable
private fun UndoButton(
    onUndo    : () -> Unit,
    onEndMatch: () -> Unit,
    modifier  : Modifier = Modifier
) {
    val scale    = LocalWatchScale.current
    val btnSize  = (44f * scale).coerceAtLeast(36f).dp
    val iconSize = (22f * scale).dp
    val xSize    = (20f * scale).dp
    val ringW    = (2.5f * scale).coerceAtLeast(1.5f).dp

    var state    by remember { mutableStateOf(UndoBtnState.ARROW) }
    var ringProgress by remember { mutableStateOf(0f) }
    val animRing by animateFloatAsState(ringProgress, label = "undoRing")
    val scope    = rememberCoroutineScope()
    var fillJob  by remember { mutableStateOf<Job?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(btnSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var longPressCompleted = false
                    fillJob?.cancel()
                    fillJob = scope.launch {
                        val totalMs = 1_500L
                        val stepMs  = 16L
                        var elapsed = 0L
                        while (elapsed < totalMs) {
                            delay(stepMs)
                            elapsed += stepMs
                            ringProgress = elapsed.toFloat() / totalMs.toFloat()
                        }
                        longPressCompleted = true
                        ringProgress = 0f
                        state = if (state == UndoBtnState.ARROW) UndoBtnState.X else UndoBtnState.ARROW
                    }
                    val up   = waitForUpOrCancellation()
                    fillJob?.cancel()
                    val wasTap = up != null && !longPressCompleted && ringProgress < 0.9f
                    ringProgress = 0f
                    if (wasTap) {
                        when (state) {
                            UndoBtnState.ARROW -> onUndo()
                            UndoBtnState.X     -> onEndMatch()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = WPColors.UndoBg)
            if (animRing > 0f) {
                val pad = 3.dp.toPx()
                drawArc(
                    color      = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * animRing,
                    useCenter  = false,
                    topLeft    = Offset(pad, pad),
                    size       = Size(size.width - pad * 2, size.height - pad * 2),
                    style      = Stroke(width = ringW.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        if (state == UndoBtnState.ARROW) {
            Icon(
                imageVector     = ReverseArrowVector,
                contentDescription = "Undo",
                tint            = WPColors.OppAccent,
                modifier        = Modifier.size(iconSize)
            )
        } else {
            Icon(
                imageVector     = CloseXVector,
                contentDescription = "End Match",
                tint            = Color.White,
                modifier        = Modifier.size(xSize)
            )
        }
    }
}

private val ReverseArrowVector: ImageVector
    get() = ImageVector.Builder(
        name = "ReverseArrow",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f,  viewportHeight = 24f
    ).apply {
        path(
            fill            = null,
            stroke          = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(9f, 15f); lineTo(3f, 9f)
            moveTo(3f, 9f);  lineTo(9f, 3f)
            moveTo(3f, 9f);  lineTo(15f, 9f)
            arcToRelative(6f, 6f, 0f, false, true, 0f, 12f)
            lineTo(12f, 21f)
        }
    }.build()

private val CloseXVector: ImageVector
    get() = ImageVector.Builder(
        name = "CloseX",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f,  viewportHeight = 24f
    ).apply {
        path(
            fill            = null,
            stroke          = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(6f, 18f); lineTo(18f, 6f)
            moveTo(6f, 6f);  lineTo(18f, 18f)
        }
    }.build()

private fun formatScore(snapshot: Snapshot, team: Team): String {
    val game = snapshot.game
    if (game.mode == GameMode.TIEBREAK) {
        return if (team == Team.YOU) game.youPoints.toString() else game.oppPoints.toString()
    }
    return when (game.phase) {
        GamePhase.ADV_YOU, GamePhase.STAR_ADV_YOU -> if (team == Team.YOU) "AD" else "40"
        GamePhase.ADV_OPP, GamePhase.STAR_ADV_OPP -> if (team == Team.OPP) "AD" else "40"
        else -> {
            val pts = if (team == Team.YOU) game.youPoints else game.oppPoints
            when (pts) { 0 -> "0"; 1 -> "15"; 2 -> "30"; else -> "40" }
        }
    }
}

private fun displayedSetScores(snapshot: Snapshot): List<SetScore> {
    val scores = snapshot.match.setScores.toMutableList()
    if (snapshot.isMatchOver) return scores.filter { it.isCompleted }
    val current = SetScore(
        youGames    = snapshot.set.youGames,
        oppGames    = snapshot.set.oppGames,
        isCompleted = false
    )
    if (scores.isEmpty()) return listOf(current)
    if (scores.last().isCompleted) scores.add(current) else scores[scores.lastIndex] = current
    return scores
}

private fun wasGameWon(prev: Snapshot, next: Snapshot): Boolean =
    (prev.set.youGames + prev.set.oppGames) != (next.set.youGames + next.set.oppGames) ||
        prev.match.setsWonYou != next.match.setsWonYou ||
        prev.match.setsWonOpp != next.match.setsWonOpp ||
        next.isMatchOver
