package com.wptracker.model

/**
 * Immutable configuration set once at match start.
 *
 * @param serveOrder For SINGLES: [A1, B1]. For DOUBLES: [A1, B1, A2, B2] (fixed for the whole match).
 */
data class Config(
    val bestOf: Int,           // 3 or 5
    val ruleMode: RuleMode,
    val playMode: PlayMode,
    val serveOrder: List<Player>
)

data class SetScore(
    val youGames: Int,
    val oppGames: Int,
    val isCompleted: Boolean
)

data class MatchState(
    val setsWonYou: Int = 0,
    val setsWonOpp: Int = 0,
    val setScores: List<SetScore> = listOf(SetScore(0, 0, false)),
    val startedAt: Long = 0L,
    val endedAt: Long? = null
)

data class SetState(
    val currentSetIndex: Int = 0,
    val youGames: Int = 0,
    val oppGames: Int = 0
)

/**
 * State of the current game.
 *
 * [youPoints] / [oppPoints] are raw rally counts (0, 1, 2, …).
 * Display mapping (0→"0", 1→"15", 2→"30", 3→"40") is computed by the UI layer.
 * The rally count is also used for:
 *   - rallyIndex = youPoints + oppPoints → serve side (even=RIGHT, odd=LEFT)
 *   - diagonal position of score numbers on screen
 *   - tie-break server determination
 *
 * [starAdvCount] counts how many times advantage returned to deuce in STAR mode (0..2).
 * When it reaches 2, the next score from DEUCE triggers STAR_POINT.
 *
 * [deciderReceiveSideOverride] is set via [MatchEngine.setDeciderSide] when the receiving
 * team exercises their right to choose the return side at a Golden or Star Point.
 * It overrides the even/odd serve-side calculation for that single deciding point only,
 * and clears automatically when the game resets.
 */
data class GameState(
    val mode: GameMode = GameMode.REGULAR,
    val youPoints: Int = 0,
    val oppPoints: Int = 0,
    val phase: GamePhase = GamePhase.NORMAL,
    val starAdvCount: Int = 0,
    val deciderReceiveSideOverride: ServeSide? = null
)

/**
 * Who serves, from where, and which index in [Config.serveOrder] is current.
 *
 * [serveOrderIndex] advances by 1 (mod serveOrder.size) after each game.
 * In tie-break it is used as the start index for the TB serve rotation.
 */
data class ServeState(
    val serverTeam: Team,
    val serverPlayer: Player,
    val serveSide: ServeSide,
    val serveOrderIndex: Int = 0
)

data class StatsState(
    val totalPlayedPoints: Int = 0,
    val goldenDecidersPlayed: Int = 0,
    val goldenDecidersWonYou: Int = 0,
    val pointsWonYou: Int = 0,   // points scored by YOU across the whole match
    val breaksYou: Int = 0,      // regular games YOU won against OPP's serve
    val breaksOpp: Int = 0,      // regular games OPP won against YOUR serve
    val deuceCount: Int = 0      // total times any game reached deuce
)

/**
 * Complete, immutable match state. Every scored point produces a new Snapshot.
 * Undo = pop the last snapshot from the history stack.
 */
data class Snapshot(
    val config: Config,
    val match: MatchState,
    val set: SetState,
    val game: GameState,
    val serve: ServeState,
    val stats: StatsState,
    val isMatchOver: Boolean = false
)
