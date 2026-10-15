package ch.wastedpotential.padel.display

import ch.wastedpotential.padel.model.Team
import kotlinx.serialization.Serializable

/** Which colour pair a choice or header uses. */
public enum class Tone {
    YOU,
    OPP,
    NEUTRAL,
    ;

    public companion object {
        public fun of(team: Team): Tone = if (team == Team.YOU) YOU else OPP
    }
}

/** One tappable answer; [tag] is the id UI tests find it by. */
@Serializable
public data class Choice(val label: String, val tone: Tone, val tag: String)

public enum class PromptKind {
    /** One choice: any tap answers it (choice 0). */
    TAP,

    /** Left half = choice 0, right half = choice 1. */
    LEFT_RIGHT,
}

/** A full-screen question: during a match (serve pick, change sides, position switch) or in setup. */
@Serializable
public data class Prompt(val kind: PromptKind, val header: String, val subtitle: String, val headerTone: Tone, val choices: List<Choice>) {
    internal companion object {
        /** "Who serves?" for [team], answered left or right: the setup's last step and the doubles serve pick. */
        fun whoServes(team: Team, leftTag: String, rightTag: String) = Prompt(
            PromptKind.LEFT_RIGHT,
            UiText.WHO_SERVES,
            if (team == Team.YOU) UiText.YOUR_SIDE else UiText.OPPONENT_SIDE,
            Tone.NEUTRAL,
            listOf(Choice(UiText.LEFT, Tone.YOU, leftTag), Choice(UiText.RIGHT, Tone.OPP, rightTag)),
        )
    }
}
