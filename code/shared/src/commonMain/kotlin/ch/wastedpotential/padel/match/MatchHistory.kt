package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.engine.MatchEngine
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

public enum class AppScreen { SETUP, MATCH, SUMMARY }

/** What the watch should feel after an action; Buzz.of turns it into a vibration. */
public enum class Feedback { NONE, POINT_YOU, POINT_OPP, GAME_WON, UNDO, CONFIRM, SIDE_CHANGE }

/**
 * The undo stack of [Snapshot]s for one match. Points and the serve pick are pushed (undo returns to them);
 * the side change and position switches replace the top (undo skips them).
 * The screen is derived from the stack, so restoring a saved stack restores the screen.
 */
internal class MatchHistory private constructor(private val stack: MutableList<Snapshot>, val epoch: Long, revision: Long) {

    /** A fresh history (first launch, reinstall, cleared data) draws a new epoch: its revisions start over. */
    constructor() : this(mutableListOf(), Random.nextLong(), 0)

    /** Counts the changes, across matches too, and is saved with the stack: the phone orders scoreboards by it. */
    var revision: Long = revision
        private set

    val current: Snapshot? get() = stack.lastOrNull()
    val size: Int get() = stack.size

    val screen: AppScreen get() {
        val snapshot = current ?: return AppScreen.SETUP
        return if (snapshot.match.endedAt != null) AppScreen.SUMMARY else AppScreen.MATCH
    }

    val canUndo: Boolean get() = stack.size > 1 && screen == AppScreen.MATCH

    /** The match was won and the summary has not been shown yet. */
    val isAwaitingSummary: Boolean get() = current?.run { isMatchOver && match.endedAt == null } == true

    fun start(config: Config, nowMs: Long) {
        stack.clear()
        push(MatchEngine.initialSnapshot(config, nowMs))
    }

    fun score(team: Team): Feedback {
        val previous = current ?: return Feedback.NONE
        val next = MatchEngine.score(previous, team)
        if (next === previous) return Feedback.NONE
        push(next)
        return when {
            next.set != previous.set -> Feedback.GAME_WON
            team == Team.YOU -> Feedback.POINT_YOU
            else -> Feedback.POINT_OPP
        }
    }

    fun undo(): Feedback {
        if (!canUndo) return Feedback.NONE
        pop()
        return Feedback.UNDO
    }

    /** Answers the prompt MatchDisplay.prompt shows: [choice] indexes its choices (0 is the left or only one). */
    fun answerPrompt(choice: Int): Feedback {
        val snapshot = current?.takeIf { screen == AppScreen.MATCH } ?: return Feedback.NONE
        val prompt = MatchDisplay.prompt(snapshot) ?: return Feedback.NONE
        if (choice !in prompt.choices.indices) return Feedback.NONE
        val first = choice == 0
        when {
            snapshot.awaitingServePick ->
                push(MatchEngine.pickOpponentFirstServer(snapshot, Player.of(snapshot.serve.serverTeam, leftSide = first)))

            snapshot.awaitingCourtSideChange -> {
                replaceTop(MatchEngine.acknowledgeCourtSideChange(snapshot))
                return Feedback.SIDE_CHANGE
            }

            snapshot.awaitingYouPositionSwitch -> replaceTop(MatchEngine.confirmPositionSwitch(snapshot, Team.YOU, first))

            else -> replaceTop(MatchEngine.confirmPositionSwitch(snapshot, Team.OPP, first))
        }
        return Feedback.CONFIRM
    }

    /** Ends the match (won or stopped early) and shows the summary; a no-op once ended. */
    fun end(nowMs: Long) {
        val snapshot = current ?: return
        if (snapshot.match.endedAt == null) replaceTop(snapshot.copy(match = snapshot.match.copy(endedAt = nowMs)))
    }

    /** Leaves the summary: undoes the winning point, or resumes a match that was stopped early. */
    fun reopen() {
        val snapshot = current ?: return
        if (snapshot.match.endedAt == null) return
        if (snapshot.isMatchOver && stack.size > 1) {
            pop()
        } else {
            replaceTop(snapshot.copy(match = snapshot.match.copy(endedAt = null)))
        }
    }

    /** Back to setup; the revision carries on, so the phone accepts the next match. */
    fun clear() {
        if (stack.isEmpty()) return
        stack.clear()
        revision++
    }

    fun encode(): String = json.encodeToString(Saved.serializer(), Saved(FORMAT_VERSION, epoch, revision, stack.toList()))

    private fun push(snapshot: Snapshot) {
        stack += snapshot
        revision++
    }

    private fun pop() {
        stack.removeAt(stack.lastIndex)
        revision++
    }

    private fun replaceTop(snapshot: Snapshot) {
        if (snapshot == stack.last()) return
        stack[stack.lastIndex] = snapshot
        revision++
    }

    @Serializable
    private class Saved(val version: Int, val epoch: Long, val revision: Long, val snapshots: List<Snapshot>)

    companion object {
        private const val FORMAT_VERSION = 3
        private val json = Json { ignoreUnknownKeys = true }

        /** Restores a saved history; null when the data is unreadable or from another format version. */
        fun decode(data: String): MatchHistory? = try {
            json.decodeFromString(Saved.serializer(), data)
                .takeIf { it.version == FORMAT_VERSION }
                ?.let { MatchHistory(it.snapshots.toMutableList(), it.epoch, it.revision) }
        } catch (_: Exception) {
            null
        }
    }
}
