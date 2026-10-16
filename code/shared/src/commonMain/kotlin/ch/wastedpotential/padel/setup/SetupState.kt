package ch.wastedpotential.padel.setup

import ch.wastedpotential.padel.display.Choice
import ch.wastedpotential.padel.display.Prompt
import ch.wastedpotential.padel.display.Tone
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.match.MatchSetup
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team

/**
 * One setup screen: a stack of [choices] with a pill on each divider, or a left/right [picker].
 * Apps render it as-is and answer with MatchSession.chooseSetup.
 */
public data class SetupScreen(val choices: List<Choice>, val pills: List<String>, val picker: Prompt?)

internal enum class SetupStep { PLAY_MODE, FORMAT, RULE_MODE, WHO_SERVES, WHICH_PLAYER }

/** The setup flow as an immutable value: choose an answer to get the next state, until [config] is set. */
internal data class SetupState(
    val step: SetupStep = SetupStep.PLAY_MODE,
    val playMode: PlayMode = PlayMode.SINGLES,
    val bestOf: Int = 3,
    val ruleMode: RuleMode = RuleMode.STANDARD,
    val servingTeam: Team = Team.YOU,
    /** Set by the last answer: start the match with it. */
    val config: Config? = null,
) {
    /** A choice and the state it leads to: the screen and its answers come from this one list. */
    private class Option(val choice: Choice, val next: () -> SetupState)

    private val options: List<Option> get() = when (step) {
        SetupStep.PLAY_MODE -> listOf(
            Option(Choice(UiText.SINGLES, Tone.OPP, UiTags.PLAY_SINGLES)) { copy(step = SetupStep.FORMAT, playMode = PlayMode.SINGLES) },
            Option(Choice(UiText.DOUBLES, Tone.YOU, UiTags.PLAY_DOUBLES)) { copy(step = SetupStep.FORMAT, playMode = PlayMode.DOUBLES) },
        )

        SetupStep.FORMAT -> listOf(
            Option(Choice(UiText.BEST_OF_3, Tone.OPP, UiTags.BEST_OF_3)) { copy(step = SetupStep.RULE_MODE, bestOf = 3) },
            Option(Choice(UiText.BEST_OF_5, Tone.YOU, UiTags.BEST_OF_5)) { copy(step = SetupStep.RULE_MODE, bestOf = 5) },
        )

        SetupStep.RULE_MODE -> listOf(
            Option(Choice(UiText.RULE_STANDARD, Tone.OPP, UiTags.RULE_STANDARD)) {
                copy(step = SetupStep.WHO_SERVES, ruleMode = RuleMode.STANDARD)
            },
            Option(Choice(UiText.RULE_GOLDEN, Tone.NEUTRAL, UiTags.RULE_GOLDEN)) {
                copy(step = SetupStep.WHO_SERVES, ruleMode = RuleMode.GOLDEN)
            },
            Option(Choice(UiText.RULE_STAR, Tone.YOU, UiTags.RULE_STAR)) { copy(step = SetupStep.WHO_SERVES, ruleMode = RuleMode.STAR) },
        )

        SetupStep.WHO_SERVES -> listOf(
            Option(Choice(UiText.SERVE_OPP, Tone.OPP, UiTags.SERVE_OPP)) { served(Team.OPP) },
            Option(Choice(UiText.SERVE_YOU, Tone.YOU, UiTags.SERVE_YOU)) { served(Team.YOU) },
        )

        SetupStep.WHICH_PLAYER -> picker.choices.mapIndexed { i, choice -> Option(choice) { finish(servingTeam, serverOnLeft = i == 0) } }
    }

    private val picker: Prompt get() = Prompt.whoServes(servingTeam, UiTags.SERVER_LEFT, UiTags.SERVER_RIGHT)

    val screen: SetupScreen get() {
        val choices = options.map { it.choice }
        return when (step) {
            SetupStep.PLAY_MODE -> SetupScreen(choices, listOf(UiText.PLAY_MODE), null)
            SetupStep.FORMAT -> SetupScreen(choices, listOf(UiText.FORMAT), null)
            SetupStep.RULE_MODE -> SetupScreen(choices, listOf(UiText.GAME, UiText.MODE), null)
            SetupStep.WHO_SERVES -> SetupScreen(choices, listOf(UiText.WHO_SERVES), null)
            SetupStep.WHICH_PLAYER -> SetupScreen(emptyList(), emptyList(), picker)
        }
    }

    /** Answers the current screen; [index] counts its choices top to bottom (or left, right). Null when out of range. */
    fun choose(index: Int): SetupState? = options.getOrNull(index)?.next?.invoke()

    /** False on the first step, where back should leave the app. */
    val canGoBack: Boolean get() = step != SetupStep.PLAY_MODE

    /** The previous step, or null on the first one. */
    fun back(): SetupState? = if (canGoBack) copy(step = SetupStep.entries[step.ordinal - 1], config = null) else null

    /** Singles is set up once the server's team is known; doubles still asks which player serves. */
    private fun served(team: Team) = if (playMode == PlayMode.SINGLES) {
        finish(team, serverOnLeft = false)
    } else {
        copy(step = SetupStep.WHICH_PLAYER, servingTeam = team)
    }

    private fun finish(team: Team, serverOnLeft: Boolean) =
        copy(servingTeam = team, config = MatchSetup.config(playMode, bestOf, ruleMode, team, serverOnLeft))
}
