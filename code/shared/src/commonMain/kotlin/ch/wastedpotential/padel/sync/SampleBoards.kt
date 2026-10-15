package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.match.MatchHistory
import ch.wastedpotential.padel.match.MatchSetup
import ch.wastedpotential.padel.match.SessionState
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.setup.SetupState
import kotlin.time.Duration.Companion.minutes

/** Sample scoreboards for the phones' UI-test and debug launch modes, built with the real engine. */
public object SampleBoards {
    public const val MATCH: String = "match" // 40:15 in the first game
    public const val SUMMARY: String = "summary" // 6-0 6-0 win in 75 minutes

    /** The sample called [name]; null for an unknown name. */
    public fun make(name: String): Scoreboard? {
        val history = MatchHistory()
        history.start(MatchSetup.config(PlayMode.SINGLES, bestOf = 3, RuleMode.STANDARD, Team.YOU, serverOnLeft = false), 0)
        when (name) {
            MATCH -> listOf(Team.YOU, Team.YOU, Team.OPP, Team.YOU).forEach { history.score(it) }

            SUMMARY -> {
                repeat(POINTS_FOR_A_LOVE_SET) { history.score(Team.YOU) }
                history.answerPrompt(0)
                repeat(POINTS_FOR_A_LOVE_SET) { history.score(Team.YOU) }
                history.end(SUMMARY_MINUTES.minutes.inWholeMilliseconds)
            }

            else -> return null
        }
        return Scoreboard.of(SessionState.of(history, SetupState()), history.epoch, history.revision)
    }

    private const val POINTS_FOR_A_LOVE_SET = 24 // 6 games of 4 points
    private const val SUMMARY_MINUTES = 75
}
