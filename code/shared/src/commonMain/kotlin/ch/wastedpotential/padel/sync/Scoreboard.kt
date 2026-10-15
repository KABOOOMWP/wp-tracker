package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.match.SessionState
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a paired phone shows: the watch sends one after every change.
 * Display-ready, so the phone renders the same [MatchDisplay] and [SummaryStats] as the watch.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class Scoreboard(
    val screen: AppScreen,
    val match: MatchDisplay? = null,
    val summary: SummaryStats? = null,
    /** Identifies the watch's revision count: a reinstalled watch starts a new one, from a low revision. */
    val epoch: Long,
    /** The watch's change counter: it only grows, whatever its clock does, so the phone keeps the highest. */
    val revision: Long,
) {
    /** The payload format; a phone drops boards of any other version. */
    @Required @EncodeDefault
    public val version: Int = FORMAT_VERSION

    /** True when this board replaces [other]: none yet, another epoch (a reinstalled watch), or not older in the same epoch. */
    internal fun supersedes(other: Scoreboard?): Boolean = other == null || epoch != other.epoch || revision >= other.revision

    public fun encode(): String = json.encodeToString(serializer(), this)

    public companion object {
        internal const val FORMAT_VERSION = 4
        private val json = Json { ignoreUnknownKeys = true }

        internal fun of(state: SessionState, epoch: Long, revision: Long) =
            Scoreboard(state.screen, state.display, state.summary, epoch, revision)

        /** Null for unreadable data and for another payload version. */
        public fun decode(data: String): Scoreboard? = try {
            json.decodeFromString(serializer(), data).takeIf { it.version == FORMAT_VERSION }
        } catch (_: Exception) {
            null
        }
    }
}
