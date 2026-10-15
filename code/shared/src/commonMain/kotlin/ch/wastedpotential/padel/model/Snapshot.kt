package ch.wastedpotential.padel.model

import kotlinx.serialization.Serializable

private const val BEST_OF_THREE = 3
private const val BEST_OF_FIVE = 5
private const val SINGLES_SERVERS = 2
private const val DOUBLES_SERVERS = 4

/**
 * The match format chosen in setup. [serveOrder] holds the servers in turn: [first, receiver] in singles,
 * [first, opponent, partner, opponent's partner] in doubles (the serve pick and position switches rewrite it).
 */
@Serializable
public data class Config(val bestOf: Int, val ruleMode: RuleMode, val playMode: PlayMode, val serveOrder: List<Player>) {
    init {
        require(bestOf == BEST_OF_THREE || bestOf == BEST_OF_FIVE) { "bestOf must be 3 or 5, was $bestOf" }
        val servers = if (playMode == PlayMode.SINGLES) SINGLES_SERVERS else DOUBLES_SERVERS
        require(serveOrder.size == servers) { "serveOrder $serveOrder does not fit $playMode" }
    }

    internal val setsToWin: Int get() = bestOf / 2 + 1
}

/** The games of one set; [isCompleted] once the set was won, never for a set cut short by "End match". */
@Serializable
public data class SetScore(val youGames: Int, val oppGames: Int, val isCompleted: Boolean) {
    public fun games(team: Team): Int = if (team == Team.YOU) youGames else oppGames

    /** The team that won the set; null while it is unfinished, also after the match was ended early. */
    public val winner: Team? get() = when {
        !isCompleted -> null
        youGames > oppGames -> Team.YOU
        else -> Team.OPP
    }

    /** True when [team] lost this set; false for one it won and for one unfinished or cut short. */
    public fun lostBy(team: Team): Boolean = winner != null && winner != team

    /** A completed set decided by a tie-break (7:6). */
    internal val wasTieBreak: Boolean get() = isCompleted && minOf(youGames, oppGames) == 6
}

@Serializable
internal data class MatchState(
    /** The completed sets, oldest first; the live set is in [Snapshot.set]. */
    val completedSets: List<SetScore> = emptyList(),
    val startedAt: Long = 0L,
    /** Set when the match is ended (won or stopped early). */
    val endedAt: Long? = null,
) {
    fun setsWon(team: Team): Int = completedSets.count { it.winner == team }
}

@Serializable
internal data class SetState(val youGames: Int = 0, val oppGames: Int = 0) {
    fun games(team: Team): Int = if (team == Team.YOU) youGames else oppGames
    fun plusGame(team: Team): SetState = if (team == Team.YOU) copy(youGames = youGames + 1) else copy(oppGames = oppGames + 1)
}

/**
 * The current game. Points are raw rally counts; their sum (the [rally]) sets the serve side, the score corners
 * and the tie-break server. [starAdvCount] counts lost Star advantages (2 → STAR_POINT).
 */
@Serializable
internal data class GameState(
    val mode: GameMode = GameMode.REGULAR,
    val youPoints: Int = 0,
    val oppPoints: Int = 0,
    val phase: GamePhase = GamePhase.NORMAL,
    val starAdvCount: Int = 0,
) {
    val rally: Int get() = youPoints + oppPoints
    fun points(team: Team): Int = if (team == Team.YOU) youPoints else oppPoints
    fun plusPoint(team: Team): GameState = if (team == Team.YOU) copy(youPoints = youPoints + 1) else copy(oppPoints = oppPoints + 1)
}

/**
 * Who serves and from which side. [serveOrderIndex] is the index of the game's server in [Config.serveOrder]
 * and advances by one after each game; in a tie-break it stays where the tie-break started.
 */
@Serializable
internal data class ServeState(
    val serverPlayer: Player,
    val serveSide: ServeSide = ServeSide.RIGHT,
    val serveOrderIndex: Int = 0,
    /** Doubles: the opponents said who serves their first game (asked before game 2). */
    val opponentServerConfirmed: Boolean = false,
) {
    val serverTeam: Team get() = serverPlayer.team
}

@Serializable
internal data class StatsState(
    val totalPlayedPoints: Int = 0,
    val pointsWonYou: Int = 0,
    // Golden and Star deciding points
    val decidersPlayed: Int = 0,
    val decidersWonYou: Int = 0,
    // regular games YOU won on the opponent's serve
    val breaksYou: Int = 0,
    val breaksOpp: Int = 0,
    // points that left the game level at 40:40 or beyond (GamePhase.isDeuce)
    val deuceCount: Int = 0,
)

/**
 * The whole match after one action; MatchEngine maps one to the next and MatchHistory stacks them for undo.
 * While an awaiting flag is set, MatchDisplay.prompt asks it and no point can be played.
 */
@Serializable
internal data class Snapshot(
    val config: Config,
    val match: MatchState,
    val set: SetState,
    val game: GameState,
    val serve: ServeState,
    val stats: StatsState,
    /** Doubles, after game 1: which opponent serves game 2? Answered by MatchEngine.pickOpponentFirstServer. */
    val awaitingServePick: Boolean = false,
    /** After every set but the last: "change sides". Answered by MatchEngine.acknowledgeCourtSideChange. */
    val awaitingCourtSideChange: Boolean = false,
    /** Doubles, after the side change: does each team switch positions? Answered by MatchEngine.confirmPositionSwitch. */
    val awaitingYouPositionSwitch: Boolean = false,
    val awaitingOppPositionSwitch: Boolean = false,
) {
    /** Won: a team has won the sets it needs. */
    val isMatchOver: Boolean get() = Team.entries.any { match.setsWon(it) >= config.setsToWin }

    val isAwaitingAnswer: Boolean
        get() = awaitingServePick || awaitingCourtSideChange || awaitingYouPositionSwitch || awaitingOppPositionSwitch

    /** A point can be played: the match is neither won nor ended, and no prompt is open. */
    val acceptsPoints: Boolean get() = !isMatchOver && match.endedAt == null && !isAwaitingAnswer
}
