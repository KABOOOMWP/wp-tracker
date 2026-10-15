package ch.wastedpotential.padel.display

import ch.wastedpotential.padel.engine.MatchEngine
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GameState
import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.ScorePosition
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.serialization.Serializable

/** Everything the match screen renders, so every app draws the same thing. */
@Serializable
public data class MatchDisplay(
    val youScore: String,
    val oppScore: String,
    /** The scores sit diagonally: the opponent's is on the other side. */
    val youScoreOnLeft: Boolean,
    /** Golden/Star deciding point: both scores are centred. */
    val scoresCentered: Boolean,
    val servingTeam: Team,
    val serveStripeOnLeft: Boolean,
    val pillLabel: String?,
    /** The completed sets, then the live one; SetScore.winner says who took each. */
    val setScores: List<SetScore>,
    /** The question covering the match, if any; answer it with MatchSession.answerPrompt. */
    val prompt: Prompt?,
) {
    public fun score(team: Team): String = if (team == Team.YOU) youScore else oppScore

    public fun scoreOnLeft(team: Team): Boolean = if (team == Team.YOU) youScoreOnLeft else !youScoreOnLeft

    /** The board as a screen reader says it: both scores, the pill, then the games of each set. */
    public fun spokenScore(names: TeamNames): String {
        val scores = Team.entries.map { "${names.name(it)} ${score(it)}" }
        val sets = setScores.map { "${it.youGames}–${it.oppGames}" }
        return (scores + listOfNotNull(pillLabel) + sets).joinToString(UiText.SPOKEN_SEPARATOR)
    }

    public companion object {
        internal fun of(snapshot: Snapshot): MatchDisplay {
            val layout = MatchEngine.computeScoreLayout(snapshot)
            return MatchDisplay(
                youScore = scoreText(snapshot.game, Team.YOU),
                oppScore = scoreText(snapshot.game, Team.OPP),
                youScoreOnLeft = layout.youPosition == ScorePosition.BOTTOM_LEFT,
                scoresCentered = snapshot.game.phase.isDecider,
                servingTeam = snapshot.serve.serverTeam,
                serveStripeOnLeft = snapshot.serve.serverPlayer.isLeftSide,
                pillLabel = pillLabel(MatchEngine.computePill(snapshot)),
                setScores = setScores(snapshot),
                prompt = prompt(snapshot),
            )
        }

        /** 0/15/30/40/AD in a regular game, the raw points in a tie-break. */
        internal fun scoreText(game: GameState, team: Team): String {
            if (game.mode == GameMode.TIEBREAK) return game.points(team).toString()
            val advantage = game.phase.advantage
            if (advantage != null) return if (advantage == team) UiText.ADVANTAGE else "40"
            return when (game.points(team)) {
                0 -> "0"
                1 -> "15"
                2 -> "30"
                else -> "40"
            }
        }

        internal fun pillLabel(pill: PillState): String? = when (pill) {
            PillState.MATCH_POINT -> UiText.MATCH_POINT
            PillState.SET_POINT -> UiText.SET_POINT
            PillState.GOLDEN_POINT -> UiText.GOLDEN_POINT
            PillState.STAR_POINT -> UiText.STAR_POINT
            PillState.BREAK_POINT -> UiText.BREAK_POINT
            PillState.TIEBREAK -> UiText.TIE_BREAK
            PillState.HIDDEN -> null
        }

        /** The completed sets plus the live games of the current set (none once the match is won). */
        internal fun setScores(snapshot: Snapshot): List<SetScore> {
            val completed = snapshot.match.completedSets
            if (snapshot.isMatchOver) return completed
            return completed + SetScore(snapshot.set.youGames, snapshot.set.oppGames, isCompleted = false)
        }

        /** The open prompt, if any. Choice 0 is always the left or only answer. */
        internal fun prompt(snapshot: Snapshot): Prompt? = when {
            snapshot.awaitingServePick -> Prompt.whoServes(snapshot.serve.serverTeam, UiTags.PICK_LEFT, UiTags.PICK_RIGHT)

            snapshot.awaitingCourtSideChange -> Prompt(
                PromptKind.TAP,
                UiText.CHANGE_SIDES,
                UiText.TAP_TO_CONTINUE,
                Tone.NEUTRAL,
                listOf(Choice(UiText.TAP_TO_CONTINUE, Tone.NEUTRAL, UiTags.COURT_SIDE_ACK)),
            )

            snapshot.awaitingYouPositionSwitch -> positionSwitch(Team.YOU)

            snapshot.awaitingOppPositionSwitch -> positionSwitch(Team.OPP)

            else -> null
        }

        private fun positionSwitch(team: Team) = Prompt(
            PromptKind.LEFT_RIGHT,
            if (team == Team.YOU) UiText.YOUR_TEAM else UiText.OPP_TEAM,
            UiText.SWITCH_SIDES,
            Tone.of(team),
            listOf(Choice(UiText.SWITCH, Tone.YOU, UiTags.SWITCH_YES), Choice(UiText.KEEP, Tone.OPP, UiTags.SWITCH_NO)),
        )
    }
}
