package com.wptracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.wptracker.engine.MatchEngine
import com.wptracker.model.*
import com.wptracker.presentation.theme.LocalIsRoundScreen
import com.wptracker.presentation.theme.LocalWatchScale

// ── Composition local wrapper ─────────────────────────────────────────────────

@Composable
fun TestWrapper(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalWatchScale    provides 1f,
        LocalIsRoundScreen provides false,
        content            = content
    )
}

// ── Config builders ───────────────────────────────────────────────────────────

fun singlesConfig(
    bestOf   : Int      = 3,
    ruleMode : RuleMode = RuleMode.STANDARD,
    youServe : Boolean  = true
) = Config(
    bestOf      = bestOf,
    ruleMode    = ruleMode,
    playMode    = PlayMode.SINGLES,
    serveOrder  = if (youServe) listOf(Player.A1, Player.B1) else listOf(Player.B1, Player.A1)
)

fun doublesConfig(
    bestOf   : Int      = 3,
    ruleMode : RuleMode = RuleMode.STANDARD,
    youServe : Boolean  = true
) = Config(
    bestOf      = bestOf,
    ruleMode    = ruleMode,
    playMode    = PlayMode.DOUBLES,
    serveOrder  = if (youServe) listOf(Player.A1, Player.B1, Player.A2, Player.B2)
                  else          listOf(Player.B1, Player.A1, Player.B2, Player.A2)
)

// ── Snapshot helpers ──────────────────────────────────────────────────────────

/** Builds a starting snapshot from config (mirrors MatchViewModel.init). */
fun startSnapshot(config: Config): Snapshot {
    val first = config.serveOrder.first()
    return Snapshot(
        config = config,
        match  = MatchState(startedAt = 0L),
        set    = SetState(),
        game   = GameState(),
        serve  = ServeState(
            serverTeam      = first.team(),
            serverPlayer    = first,
            serveSide       = ServeSide.RIGHT,
            serveOrderIndex = 0
        ),
        stats  = StatsState()
    )
}

/** Scores [count] points for [team] from [snap]. */
fun scorePoints(snap: Snapshot, team: Team, count: Int): Snapshot {
    var s = snap
    repeat(count) { s = MatchEngine.score(s, team) }
    return s
}

/**
 * Plays a complete 6-0 set for [winner].
 * Each game is won 4-0 (no deuce) so the set finishes cleanly.
 */
fun playSet(snap: Snapshot, winner: Team): Snapshot =
    scorePoints(snap, winner, 24) // 6 games × 4 points

/**
 * Returns a snapshot where [winner] has won the match 2-0 (sets).
 * Useful as input to SummaryScreen tests.
 */
fun completedMatch(config: Config = singlesConfig(), winner: Team = Team.YOU): Snapshot {
    var s = startSnapshot(config).copy(match = MatchState(startedAt = 0L, endedAt = 1_000L))
    s = playSet(s, winner)
    s = playSet(s, winner)
    // Force match-over flag so SummaryScreen receives a proper snapshot
    return s.copy(isMatchOver = true, match = s.match.copy(endedAt = 1_000L))
}
