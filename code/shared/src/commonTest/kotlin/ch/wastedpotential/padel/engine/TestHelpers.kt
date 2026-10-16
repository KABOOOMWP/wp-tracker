package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.GameState
import ch.wastedpotential.padel.model.MatchState
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.ServeState
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.StatsState
import ch.wastedpotential.padel.model.Team

// ---------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------

internal fun singlesConfig(ruleMode: RuleMode = RuleMode.STANDARD, bestOf: Int = 3) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.SINGLES,
    serveOrder = listOf(Player.A1, Player.B1),
)

internal fun doublesConfig(ruleMode: RuleMode = RuleMode.STANDARD, bestOf: Int = 3) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.DOUBLES,
    serveOrder = listOf(Player.A1, Player.B1, Player.A2, Player.B2),
)

/** Singles starting with the opponent: B1 → A1 → B1 → … */
internal fun singlesConfigOppFirst(ruleMode: RuleMode = RuleMode.STANDARD, bestOf: Int = 3) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.SINGLES,
    serveOrder = listOf(Player.B1, Player.A1),
)

/** Doubles starting with opponent: B1 → A1 → B2 → A2 → B1 → … */
internal fun doublesConfigOppFirst(ruleMode: RuleMode = RuleMode.STANDARD, bestOf: Int = 3) = Config(
    bestOf = bestOf,
    ruleMode = ruleMode,
    playMode = PlayMode.DOUBLES,
    serveOrder = listOf(Player.B1, Player.A1, Player.B2, Player.A2),
)

/** A snapshot at the given score. Won sets are recorded as 6-0 (or 0-6); the match is won once enough are given. */
internal fun makeSnapshot(
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
    serverTeam: Team? = null,
    serverPlayer: Player? = null,
    serveOrderIndex: Int = 0,
): Snapshot {
    val server = serverPlayer ?: serverTeam?.let { Player.of(it, leftSide = false) } ?: config.serveOrder[serveOrderIndex]
    require(serverTeam == null || server.team == serverTeam) { "$server does not play for $serverTeam" }
    return Snapshot(
        config = config,
        match = MatchState(completedSets = List(setsWonYou) { SetScore(6, 0, true) } + List(setsWonOpp) { SetScore(0, 6, true) }),
        set = SetScore(youGames, oppGames),
        game = GameState(mode = gameMode, youPoints = youPoints, oppPoints = oppPoints, phase = phase, starAdvCount = starAdvCount),
        serve = ServeState(serverPlayer = server, serveOrderIndex = serveOrderIndex),
        stats = StatsState(),
    )
}

// ---------------------------------------------------------------------------
// Score helpers: repeatedly call MatchEngine.score
// ---------------------------------------------------------------------------

internal fun Snapshot.score(team: Team): Snapshot = MatchEngine.score(this, team)

internal fun Snapshot.scoreMany(vararg teams: Team): Snapshot = teams.fold(this) { snap, team -> snap.score(team) }

// Shortcuts
internal fun Snapshot.you(): Snapshot = score(Team.YOU)
internal fun Snapshot.opp(): Snapshot = score(Team.OPP)
internal fun Snapshot.acknowledge(): Snapshot = MatchEngine.acknowledgeCourtSideChange(this)

/** Answers an open doubles serve pick with the opponent already in the order, so the order stays as configured. */
internal fun Snapshot.keepServeOrder(): Snapshot =
    if (awaitingServePick) MatchEngine.pickOpponentFirstServer(this, config.serveOrder[1]) else this

/** Wins the current game from love for [winner], keeping the serve order through the doubles serve pick. */
internal fun Snapshot.winGame(winner: Team): Snapshot = scoreMany(*Array(4) { winner }).keepServeOrder()

/** Wins the current set 6-0 for [winner] from 0:0; the inter-set prompts stay open. */
internal fun Snapshot.winSet(winner: Team): Snapshot = (1..6).fold(this) { s, _ -> s.winGame(winner) }

/** Wins the current set 6-0 for [winner] and answers the inter-set prompts (side change, keep positions). */
internal fun Snapshot.winSetAndContinue(winner: Team): Snapshot {
    var s = winSet(winner).acknowledge()
    if (s.awaitingYouPositionSwitch) s = MatchEngine.confirmPositionSwitch(s, Team.YOU, false)
    if (s.awaitingOppPositionSwitch) s = MatchEngine.confirmPositionSwitch(s, Team.OPP, false)
    return s
}
