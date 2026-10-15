package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.GameState
import ch.wastedpotential.padel.model.MatchState
import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.ScoreLayout
import ch.wastedpotential.padel.model.ScorePosition
import ch.wastedpotential.padel.model.ServeSide
import ch.wastedpotential.padel.model.ServeState
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.SetState
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.StatsState
import ch.wastedpotential.padel.model.Team

/**
 * The scoring rules: pure functions from one [Snapshot] to the next.
 * Each returns the same instance when the action does not apply, so callers can detect a no-op.
 */
internal object MatchEngine {

    private const val GAME_POINTS = 4
    private const val TIEBREAK_POINTS = 7
    private const val SET_GAMES = 6
    private const val STAR_ADVANTAGES = 2

    fun initialSnapshot(config: Config, startedAt: Long): Snapshot = Snapshot(
        config = config,
        match = MatchState(startedAt = startedAt),
        set = SetState(),
        game = GameState(),
        serve = serveAt(config, 0, opponentServerConfirmed = false),
        stats = StatsState(),
    )

    /** Scores a point for [team]; a no-op unless [Snapshot.acceptsPoints]. */
    fun score(snapshot: Snapshot, team: Team): Snapshot {
        if (!snapshot.acceptsPoints) return snapshot
        val game = snapshot.game
        val stats = snapshot.stats.copy(
            totalPlayedPoints = snapshot.stats.totalPlayedPoints + 1,
            pointsWonYou = snapshot.stats.pointsWonYou + if (team == Team.YOU) 1 else 0,
        )
        if (winsNextPoint(game, team)) {
            val decided = if (!game.phase.isDecider) {
                stats
            } else {
                stats.copy(
                    decidersPlayed = stats.decidersPlayed + 1,
                    decidersWonYou = stats.decidersWonYou + if (team == Team.YOU) 1 else 0,
                )
            }
            return winGame(snapshot, team, decided)
        }
        val next = advancePhase(game.plusPoint(team), snapshot.config.ruleMode, team)
        val serve = if (game.mode == GameMode.TIEBREAK) {
            tieBreakServe(snapshot, next.rally)
        } else {
            snapshot.serve.copy(serveSide = sideOf(next.rally))
        }
        return snapshot.copy(
            game = next,
            serve = serve,
            stats = if (next.phase.isDeuce) stats.copy(deuceCount = stats.deuceCount + 1) else stats,
        )
    }

    /** Doubles, before game 2: [player] serves for the opponents. A no-op unless that is asked and [player] is one of them. */
    fun pickOpponentFirstServer(snapshot: Snapshot, player: Player): Snapshot {
        if (!snapshot.awaitingServePick || player.team != snapshot.serve.serverTeam) return snapshot
        val first = snapshot.config.serveOrder.first()
        val config = snapshot.config.copy(serveOrder = listOf(first, player, first.partner, player.partner))
        return snapshot.copy(
            config = config,
            serve = serveAt(config, 1, opponentServerConfirmed = true),
            awaitingServePick = false,
        )
    }

    /** Dismisses "change sides"; in doubles both position-switch prompts follow. A no-op unless it is showing. */
    fun acknowledgeCourtSideChange(snapshot: Snapshot): Snapshot {
        if (!snapshot.awaitingCourtSideChange) return snapshot
        val isDoubles = snapshot.config.playMode == PlayMode.DOUBLES
        return snapshot.copy(
            awaitingCourtSideChange = false,
            awaitingYouPositionSwitch = isDoubles,
            awaitingOppPositionSwitch = isDoubles,
        )
    }

    /** [team] keeps or swaps its two players' positions, in the serve order and as the server. A no-op unless asked. */
    fun confirmPositionSwitch(snapshot: Snapshot, team: Team, doSwitch: Boolean): Snapshot {
        val awaiting = if (team == Team.YOU) snapshot.awaitingYouPositionSwitch else snapshot.awaitingOppPositionSwitch
        if (!awaiting) return snapshot
        val answered = if (team == Team.YOU) {
            snapshot.copy(awaitingYouPositionSwitch = false)
        } else {
            snapshot.copy(awaitingOppPositionSwitch = false)
        }
        if (!doSwitch) return answered
        fun swap(p: Player) = if (p.team == team) p.partner else p
        return answered.copy(
            config = snapshot.config.copy(serveOrder = snapshot.config.serveOrder.map(::swap)),
            serve = snapshot.serve.copy(serverPlayer = swap(snapshot.serve.serverPlayer)),
        )
    }

    /** The status pill: the first of MATCH, SET, GOLDEN, STAR, BREAK point, TIEBREAK that applies. */
    fun computePill(snapshot: Snapshot): PillState {
        val game = snapshot.game
        fun hasSetPoint(team: Team) = winsNextPoint(game, team) && setWinner(snapshot.set.plusGame(team)) == team
        fun hasMatchPoint(team: Team) = hasSetPoint(team) && snapshot.match.setsWon(team) + 1 >= snapshot.config.setsToWin
        return when {
            Team.entries.any(::hasMatchPoint) -> PillState.MATCH_POINT
            Team.entries.any(::hasSetPoint) -> PillState.SET_POINT
            game.phase == GamePhase.GOLDEN -> PillState.GOLDEN_POINT
            game.phase == GamePhase.STAR_POINT -> PillState.STAR_POINT
            winsNextPoint(game, snapshot.serve.serverTeam.other) -> PillState.BREAK_POINT
            game.mode == GameMode.TIEBREAK -> PillState.TIEBREAK
            else -> PillState.HIDDEN
        }
    }

    /** Even rally: You bottom-right, Opp top-left; odd: You bottom-left, Opp top-right. Tie-breaks too. */
    fun computeScoreLayout(snapshot: Snapshot): ScoreLayout = if (snapshot.game.rally % 2 == 0) {
        ScoreLayout(ScorePosition.BOTTOM_RIGHT, ScorePosition.TOP_LEFT)
    } else {
        ScoreLayout(ScorePosition.BOTTOM_LEFT, ScorePosition.TOP_RIGHT)
    }

    // ── Game ─────────────────────────────────────────────────────────────────

    /** True when [team] wins the game by scoring the next point; the one rule for points and the pill. */
    private fun winsNextPoint(game: GameState, team: Team): Boolean = when (game.phase) {
        GamePhase.NORMAL -> {
            val target = if (game.mode == GameMode.TIEBREAK) TIEBREAK_POINTS else GAME_POINTS
            val points = game.points(team) + 1
            points >= target && points - game.points(team.other) >= 2
        }

        GamePhase.DEUCE -> false

        GamePhase.GOLDEN, GamePhase.STAR_POINT -> true

        else -> game.phase.advantage == team
    }

    /** The phase after a point that did not win the game ([game] already counts it). */
    private fun advancePhase(game: GameState, rule: RuleMode, scorer: Team): GameState = when (game.phase) {
        GamePhase.NORMAL -> {
            val atDeuce = game.mode == GameMode.REGULAR && game.youPoints == GAME_POINTS - 1 && game.oppPoints == GAME_POINTS - 1
            if (!atDeuce) {
                game
            } else {
                game.copy(phase = if (rule == RuleMode.GOLDEN) GamePhase.GOLDEN else GamePhase.DEUCE)
            }
        }

        GamePhase.DEUCE -> game.copy(
            phase = when (rule) {
                RuleMode.STAR -> if (scorer == Team.YOU) GamePhase.STAR_ADV_YOU else GamePhase.STAR_ADV_OPP
                else -> if (scorer == Team.YOU) GamePhase.ADV_YOU else GamePhase.ADV_OPP
            },
        )

        GamePhase.ADV_YOU, GamePhase.ADV_OPP -> game.copy(phase = GamePhase.DEUCE)

        GamePhase.STAR_ADV_YOU, GamePhase.STAR_ADV_OPP -> {
            val lost = game.starAdvCount + 1
            game.copy(phase = if (lost >= STAR_ADVANTAGES) GamePhase.STAR_POINT else GamePhase.DEUCE, starAdvCount = lost)
        }

        GamePhase.GOLDEN, GamePhase.STAR_POINT -> error("a deciding point always wins the game")
    }

    // ── Set and match ────────────────────────────────────────────────────────

    private fun winGame(snapshot: Snapshot, winner: Team, stats: StatsState): Snapshot {
        val isBreak = snapshot.game.mode == GameMode.REGULAR && snapshot.serve.serverTeam != winner
        val counted = when {
            !isBreak -> stats
            winner == Team.YOU -> stats.copy(breaksYou = stats.breaksYou + 1)
            else -> stats.copy(breaksOpp = stats.breaksOpp + 1)
        }
        val set = snapshot.set.plusGame(winner)
        val confirmed = snapshot.serve.opponentServerConfirmed
        return when {
            setWinner(set) != null -> winSet(snapshot, set, counted)

            set.youGames == SET_GAMES && set.oppGames == SET_GAMES -> snapshot.copy(
                set = set,
                game = GameState(mode = GameMode.TIEBREAK),
                serve = nextGameServe(snapshot),
                stats = counted,
            )

            else -> snapshot.copy(
                set = set,
                game = GameState(),
                serve = nextGameServe(snapshot),
                stats = counted,
                awaitingServePick = snapshot.config.playMode == PlayMode.DOUBLES && snapshot.nextServerIndex == 1 && !confirmed,
            )
        }
    }

    /** First to six games by two, or 7:5, or 7:6 after the tie-break. */
    private fun setWinner(set: SetState): Team? {
        val leader = when {
            set.youGames > set.oppGames -> Team.YOU
            set.oppGames > set.youGames -> Team.OPP
            else -> return null
        }
        val games = set.games(leader)
        return leader.takeIf { games == SET_GAMES + 1 || (games >= SET_GAMES && games - set.games(leader.other) >= 2) }
    }

    private fun winSet(snapshot: Snapshot, set: SetState, stats: StatsState): Snapshot {
        val won = snapshot.copy(
            match = snapshot.match.copy(
                completedSets =
                snapshot.match.completedSets + SetScore(set.youGames, set.oppGames, isCompleted = true),
            ),
            set = set,
            stats = stats,
        )
        if (won.isMatchOver) return won
        return won.copy(
            set = SetState(),
            game = GameState(),
            serve = nextGameServe(snapshot),
            awaitingCourtSideChange = true,
        )
    }

    // ── Serve ────────────────────────────────────────────────────────────────

    /** The player at [orderIndex] serves a new game, from the right. */
    private fun serveAt(config: Config, orderIndex: Int, opponentServerConfirmed: Boolean) =
        ServeState(config.serveOrder[orderIndex], ServeSide.RIGHT, orderIndex, opponentServerConfirmed)

    /** The serve order moves on by one after every game, also after a tie-break. */
    private val Snapshot.nextServerIndex: Int get() = (serve.serveOrderIndex + 1) % config.serveOrder.size

    private fun nextGameServe(snapshot: Snapshot) =
        serveAt(snapshot.config, snapshot.nextServerIndex, snapshot.serve.opponentServerConfirmed)

    private fun sideOf(rally: Int) = if (rally % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT

    /** The tie-break's first server serves one point, then the next ones in the order serve two each; [rally] is at least 1. */
    private fun tieBreakServe(snapshot: Snapshot, rally: Int): ServeState {
        val order = snapshot.config.serveOrder
        val index = (snapshot.serve.serveOrderIndex + 1 + (rally - 1) / 2) % order.size
        return snapshot.serve.copy(serverPlayer = order[index], serveSide = sideOf(rally))
    }
}
