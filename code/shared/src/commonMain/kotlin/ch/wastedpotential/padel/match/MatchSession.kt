package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.platform.currentTimeMs
import ch.wastedpotential.padel.setup.SetupScreen
import ch.wastedpotential.padel.setup.SetupState
import ch.wastedpotential.padel.sync.Scoreboard

/** Receives every scoreboard the watch should mirror on a paired phone. */
public fun interface ScoreboardSink {
    public fun send(board: Scoreboard)
}

/** What a watch app draws right now. */
public data class SessionState(
    val screen: AppScreen,
    val display: MatchDisplay?,
    val summary: SummaryStats?,
    val canUndo: Boolean,
    /** Won, summary not shown yet: wait UiTimings.AUTO_SUMMARY_DELAY_MS, then call end(). */
    val isAwaitingSummary: Boolean,
    /** The setup screen to draw; non-null only on AppScreen.SETUP. */
    val setup: SetupScreen?,
    /** True when the system back is the app's: a step back in setup, or swallowed in a match. */
    val handlesBack: Boolean,
) {
    internal companion object {
        fun of(history: MatchHistory, setup: SetupState): SessionState {
            val snapshot = history.current
            val screen = history.screen
            return SessionState(
                screen = screen,
                display = snapshot?.takeIf { screen == AppScreen.MATCH }?.let(MatchDisplay::of),
                summary = snapshot?.takeIf { screen == AppScreen.SUMMARY }?.let(SummaryStats::of),
                canUndo = history.canUndo,
                isAwaitingSummary = history.isAwaitingSummary,
                setup = setup.screen.takeIf { screen == AppScreen.SETUP },
                handlesBack = when (screen) {
                    AppScreen.SETUP -> setup.canGoBack
                    AppScreen.MATCH -> true
                    AppScreen.SUMMARY -> false
                },
            )
        }
    }
}

/**
 * A watch app's whole match logic: the setup flow and the match, saved to [store] after every change
 * and mirrored to [phone]. Each app only observes [state] (via [onChange]) and plays the returned feedback.
 */
public class MatchSession(private val store: TextStore, private val phone: ScoreboardSink?, private val clock: () -> Long) {
    public constructor(store: TextStore, phone: ScoreboardSink?) : this(store, phone, ::currentTimeMs)

    private val history = store.load()?.let(MatchHistory::decode) ?: MatchHistory()
    private var setup = SetupState()

    /** Called after every change, on the caller's thread. */
    public var onChange: (() -> Unit)? = null

    public var state: SessionState = SessionState.of(history, setup)
        private set

    public fun start(config: Config) {
        setup = SetupState()
        change { history.start(config, clock()) }
    }

    /** Answers the current setup screen; the last answer starts the match. A late tap off setup does nothing. */
    public fun chooseSetup(index: Int): Feedback {
        if (state.screen != AppScreen.SETUP) return Feedback.NONE
        val next = setup.choose(index) ?: return Feedback.NONE
        val config = next.config
        if (config != null) {
            start(config)
        } else {
            setup = next
            refresh()
        }
        return Feedback.CONFIRM
    }

    /** The system back: steps back through setup and is swallowed in a match. False: let the app close. */
    public fun back(): Boolean {
        if (!state.handlesBack) return false
        if (state.screen == AppScreen.SETUP) {
            setup.back()?.let {
                setup = it
                refresh()
            }
        }
        return true
    }

    public fun score(team: Team): Feedback = change { history.score(team) }
    public fun undo(): Feedback = change { history.undo() }
    public fun answerPrompt(choice: Int): Feedback = change { history.answerPrompt(choice) }
    public fun end(): Unit = change { history.end(clock()) }
    public fun reopen(): Unit = change { history.reopen() }

    /** Back to the first setup step for the next match. */
    public fun newMatch() {
        setup = SetupState()
        val before = history.revision
        change { history.clear() }
        if (history.revision == before) refresh()
    }

    /** Sends the current scoreboard again, e.g. when the phone link comes up. */
    public fun announce() {
        phone?.send(Scoreboard.of(state, history.epoch, history.revision))
    }

    /** Runs [action]; when it changed the match, saves, redraws and tells the phone. */
    private inline fun <T> change(action: () -> T): T {
        val before = history.revision
        val result = action()
        if (history.revision != before) {
            store.save(history.encode())
            refresh()
            announce()
        }
        return result
    }

    /** Redraws from the history and the setup step. */
    private fun refresh() {
        state = SessionState.of(history, setup)
        onChange?.invoke()
    }
}
