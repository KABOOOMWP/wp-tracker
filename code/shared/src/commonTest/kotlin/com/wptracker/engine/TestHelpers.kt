package com.wptracker.engine

import com.wptracker.model.*

// ---------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------

fun singlesConfig(
    ruleMode: RuleMode = RuleMode.STANDARD,
    bestOf: Int = 3
) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.SINGLES,
    serveOrder = listOf(Player.A1, Player.B1)
)

fun doublesConfig(
    ruleMode: RuleMode = RuleMode.STANDARD,
    bestOf: Int = 3
) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.DOUBLES,
    serveOrder = listOf(Player.A1, Player.B1, Player.A2, Player.B2)
)

/** Singles starting with the opponent: B1 → A1 → B1 → … */
fun singlesConfigOppFirst(
    ruleMode: RuleMode = RuleMode.STANDARD,
    bestOf: Int = 3
) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.SINGLES,
    serveOrder = listOf(Player.B1, Player.A1)
)

/**
 * Doubles config with opponentServerConfirmed=false (deferred pick).
 * Identical to [doublesConfig] since opponentServerConfirmed lives on ServeState (defaults false).
 * Named separately for clarity in deferred-pick tests.
 */
fun doublesConfigDeferred(
    ruleMode: RuleMode = RuleMode.STANDARD,
    bestOf: Int = 3
) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.DOUBLES,
    serveOrder = listOf(Player.A1, Player.B1, Player.A2, Player.B2)
)

/** Doubles starting with opponent: B1 → A1 → B2 → A2 → B1 → … */
fun doublesConfigOppFirst(
    ruleMode: RuleMode = RuleMode.STANDARD,
    bestOf: Int = 3
) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.DOUBLES,
    serveOrder = listOf(Player.B1, Player.A1, Player.B2, Player.A2)
)

fun makeSnapshot(
    config: Config = singlesConfig(),
    youPoints: Int = 0,
    oppPoints: Int = 0,
    youGames: Int = 0,
    oppGames: Int = 0,
    setsWonYou: Int = 0,
    setsWonOpp: Int = 0,
    phase: GamePhase = GamePhase.NORMAL,
    starAdvCount: Int = 0,
    gameMode: GameMode = GameMode.REGULAR,
    serverTeam: Team = Team.YOU,
    serverPlayer: Player = Player.A1,
    serveSide: ServeSide = ServeSide.RIGHT,
    serveOrderIndex: Int = 0,
    isMatchOver: Boolean = false
): Snapshot {
    val setScores = mutableListOf<SetScore>()
    // Build fake completed sets based on setsWon counters (simplified: wins as 6-0)
    repeat(setsWonYou) { setScores.add(SetScore(6, 0, true)) }
    repeat(setsWonOpp) { setScores.add(SetScore(0, 6, true)) }
    setScores.add(SetScore(youGames, oppGames, false))

    return Snapshot(
        config = config,
        match = MatchState(
            setsWonYou = setsWonYou,
            setsWonOpp = setsWonOpp,
            setScores = setScores
        ),
        set = SetState(
            currentSetIndex = setsWonYou + setsWonOpp,
            youGames = youGames,
            oppGames = oppGames
        ),
        game = GameState(
            mode = gameMode,
            youPoints = youPoints,
            oppPoints = oppPoints,
            phase = phase,
            starAdvCount = starAdvCount
        ),
        serve = ServeState(
            serverTeam = serverTeam,
            serverPlayer = serverPlayer,
            serveSide = serveSide,
            serveOrderIndex = serveOrderIndex
        ),
        stats = StatsState(),
        isMatchOver = isMatchOver
    )
}

// ---------------------------------------------------------------------------
// Score helpers: repeatedly call MatchEngine.score
// ---------------------------------------------------------------------------

fun Snapshot.score(team: Team): Snapshot = MatchEngine.score(this, team)

fun Snapshot.scoreMany(vararg teams: Team): Snapshot =
    teams.fold(this) { snap, team -> snap.score(team) }

// Shortcuts
fun Snapshot.you(): Snapshot = score(Team.YOU)
fun Snapshot.opp(): Snapshot = score(Team.OPP)
