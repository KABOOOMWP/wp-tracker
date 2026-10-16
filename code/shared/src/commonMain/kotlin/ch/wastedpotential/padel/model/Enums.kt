package ch.wastedpotential.padel.model

public enum class Team {
    YOU,
    OPP,
    ;

    internal val other: Team get() = if (this == YOU) OPP else YOU
}

/** A1/A2 are your team, B1/B2 the opponents; A1/B1 play the right court side, A2/B2 the left. */
public enum class Player {
    A1,
    A2,
    B1,
    B2,
    ;

    internal val team: Team get() = if (this == A1 || this == A2) Team.YOU else Team.OPP

    internal val partner: Player get() = when (this) {
        A1 -> A2
        A2 -> A1
        B1 -> B2
        B2 -> B1
    }

    internal val isLeftSide: Boolean get() = this == A2 || this == B2

    internal companion object {
        fun of(team: Team, leftSide: Boolean): Player = when (team) {
            Team.YOU -> if (leftSide) A2 else A1
            Team.OPP -> if (leftSide) B2 else B1
        }
    }
}

public enum class PlayMode { SINGLES, DOUBLES }

public enum class RuleMode { STANDARD, GOLDEN, STAR }

internal enum class GameMode { REGULAR, TIEBREAK }

/** Where the current game stands. Advantage phases show "AD"; GOLDEN and STAR_POINT are deciding points. */
internal enum class GamePhase {
    NORMAL, // before 40:40
    DEUCE, // 40:40, or back to it after an advantage
    ADV_YOU,
    ADV_OPP, // Standard advantage
    GOLDEN, // Golden Point: 40:40, the next point wins
    STAR_ADV_YOU,
    STAR_ADV_OPP, // Star advantage (first or second)
    STAR_POINT, // Star Point: third deuce, the next point wins
    ;

    /** The next point wins the game, whoever scores it. */
    val isDecider: Boolean get() = this == GOLDEN || this == STAR_POINT

    /** Level at 40:40 or beyond, in every rule mode: the one definition of a deuce. */
    val isDeuce: Boolean get() = this == DEUCE || isDecider

    /** The team holding the advantage, or null. */
    val advantage: Team? get() = when (this) {
        ADV_YOU, STAR_ADV_YOU -> Team.YOU
        ADV_OPP, STAR_ADV_OPP -> Team.OPP
        else -> null
    }
}

/** What the status pill shows; MatchEngine.computePill picks the first that applies, from the top. */
internal enum class PillState { MATCH_POINT, SET_POINT, GOLDEN_POINT, STAR_POINT, BREAK_POINT, TIEBREAK, HIDDEN }
