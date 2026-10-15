package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.model.Team
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Team names typed on the phone and shown on both devices. */
@Serializable
public data class TeamNames(val you: String = DEFAULT_YOU, val opp: String = DEFAULT_OPP) {

    public fun name(team: Team): String = if (team == Team.YOU) you else opp

    /** The name as the summary panel draws it. */
    public fun summaryName(team: Team): String = name(team).uppercase()

    /** Applies an edit: input is trimmed, a blank field keeps the current name, and names are capped. */
    public fun edited(you: String, opp: String): TeamNames = TeamNames(
        you = you.trim().ifEmpty { this.you }.take(MAX_LENGTH),
        opp = opp.trim().ifEmpty { this.opp }.take(MAX_LENGTH),
    )

    public fun encode(): String = json.encodeToString(serializer(), this)

    public companion object {
        public const val DEFAULT_YOU: String = UiText.YOU
        public const val DEFAULT_OPP: String = UiText.OPPONENT
        public const val MAX_LENGTH: Int = 24
        private val json = Json { ignoreUnknownKeys = true }

        public val DEFAULT: TeamNames = TeamNames()

        public fun decode(data: String): TeamNames? = try {
            json.decodeFromString(serializer(), data)
        } catch (_: Exception) {
            null
        }
    }
}
