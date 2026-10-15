package ch.wastedpotential.padel.display

import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/** One summary line: a label and its value ("24 – 18" for the YOU – OPP rows). */
@Serializable
public data class StatRow(val label: String, val value: String)

/** The numbers on the match summary screen. */
@Serializable
public data class SummaryStats(
    /** Who won more sets; null when the match was stopped early with the sets level. */
    val winner: Team?,
    val resultLabel: String,
    /** The sets played, including a set cut short; SetScore.winner is null for that one. */
    val sets: List<SetScore>,
    val setsWonYou: Int,
    val setsWonOpp: Int,
    val pointsYou: Int,
    val pointsOpp: Int,
    val gamesYou: Int,
    val gamesOpp: Int,
    val breaksYou: Int,
    val breaksOpp: Int,
    val avgPointsPerGame: Int,
    val tiebreaks: Int,
    val deuces: Int,
    val deciderLabel: String?,
    val decidersWonYou: Int,
    val decidersPlayed: Int,
    val duration: String,
    /** True when the match was won by a point; reopening then undoes that point. */
    val wasWon: Boolean,
) {
    public fun setsWon(team: Team): Int = if (team == Team.YOU) setsWonYou else setsWonOpp

    /** YOU – OPP rows, drawn under a "YOU – OPP" header. */
    public val splitRows: List<StatRow> get() = listOf(
        StatRow(UiText.STAT_POINTS, "$pointsYou – $pointsOpp"),
        StatRow(UiText.STAT_GAMES, "$gamesYou – $gamesOpp"),
        StatRow(UiText.STAT_BREAKS, "$breaksYou – $breaksOpp"),
    )

    /** Single-value rows; tiebreaks, deuces and deciders only when they happened. */
    public val detailRows: List<StatRow> get() = listOfNotNull(
        StatRow(UiText.STAT_DURATION, duration),
        StatRow(UiText.STAT_AVG_POINTS, avgPointsPerGame.toString()),
        StatRow(UiText.STAT_TIEBREAKS, tiebreaks.toString()).takeIf { tiebreaks > 0 },
        StatRow(UiText.STAT_DEUCES, deuces.toString()).takeIf { deuces > 0 },
        deciderLabel?.let { StatRow(it, "$decidersWonYou / $decidersPlayed") },
    )

    /** The summary's reopen button: undo the winning point, or resume a stopped match. */
    public val reopenLabel: String get() = if (wasWon) UiText.UNDO_LAST_POINT else UiText.RESUME_MATCH

    public companion object {
        /** The summary of an ended match: [snapshot] must carry its `endedAt`. */
        internal fun of(snapshot: Snapshot): SummaryStats {
            val match = snapshot.match
            val endedAt = checkNotNull(match.endedAt) { "the summary needs an ended match" }
            val stats = snapshot.stats
            val sets = MatchDisplay.setScores(snapshot).filter { it.isCompleted || it.youGames + it.oppGames > 0 }
            val gamesYou = sets.sumOf { it.youGames }
            val gamesOpp = sets.sumOf { it.oppGames }
            val setsWonYou = match.setsWon(Team.YOU)
            val setsWonOpp = match.setsWon(Team.OPP)
            val winner = when {
                setsWonYou > setsWonOpp -> Team.YOU
                setsWonOpp > setsWonYou -> Team.OPP
                else -> null
            }
            val deciderLabel = when {
                stats.decidersPlayed == 0 -> null
                snapshot.config.ruleMode == RuleMode.GOLDEN -> UiText.STAT_GOLDEN_WON
                snapshot.config.ruleMode == RuleMode.STAR -> UiText.STAT_STAR_WON
                else -> null
            }
            return SummaryStats(
                winner = winner,
                resultLabel = when (winner) {
                    Team.YOU -> UiText.YOU_WIN
                    Team.OPP -> UiText.OPPONENT_WINS
                    null -> UiText.MATCH_ENDED
                },
                sets = sets,
                setsWonYou = setsWonYou,
                setsWonOpp = setsWonOpp,
                pointsYou = stats.pointsWonYou,
                pointsOpp = stats.totalPlayedPoints - stats.pointsWonYou,
                gamesYou = gamesYou,
                gamesOpp = gamesOpp,
                breaksYou = stats.breaksYou,
                breaksOpp = stats.breaksOpp,
                avgPointsPerGame = if (gamesYou + gamesOpp > 0) stats.totalPlayedPoints / (gamesYou + gamesOpp) else 0,
                tiebreaks = sets.count { it.wasTieBreak },
                deuces = stats.deuceCount,
                deciderLabel = deciderLabel,
                decidersWonYou = stats.decidersWonYou,
                decidersPlayed = stats.decidersPlayed,
                duration = formatDuration(endedAt - match.startedAt),
                wasWon = snapshot.isMatchOver,
            )
        }

        /** HH:MM. */
        internal fun formatDuration(durationMs: Long): String {
            fun twoDigits(v: Number) = v.toString().padStart(2, '0')
            return maxOf(0L, durationMs).milliseconds.toComponents { hours, minutes, _, _ -> "${twoDigits(hours)}:${twoDigits(minutes)}" }
        }
    }
}
