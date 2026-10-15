package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.match.AppScreen

/** What a phone shows: the newest scoreboard from the watch. */
public class PhoneBoard {

    public var board: Scoreboard? = null
        private set

    /** The live match to draw; null when the watch is not on one. */
    public val match: MatchDisplay? get() = board?.takeIf { it.screen == AppScreen.MATCH }?.match

    /** The summary to draw; null when the watch is not on it. Neither: the waiting board. */
    public val summary: SummaryStats? get() = board?.takeIf { it.screen == AppScreen.SUMMARY }?.summary

    /** The name-editor button is hidden on the summary, where it would cover the stats. */
    public val showsNameEditor: Boolean get() = summary == null

    /** Accepts a scoreboard; unreadable ones and older revisions of the same epoch are ignored. Returns true when shown. */
    public fun receive(data: String): Boolean {
        val incoming = Scoreboard.decode(data) ?: return false
        if (!incoming.supersedes(board)) return false
        board = incoming
        return true
    }
}
