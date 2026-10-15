package ch.wastedpotential.padel.presentation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.link.SyncChannel
import ch.wastedpotential.padel.match.Feedback
import ch.wastedpotential.padel.match.MatchSession
import ch.wastedpotential.padel.match.ScoreboardSink
import ch.wastedpotential.padel.match.SessionState
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.platform.currentTimeMs
import ch.wastedpotential.padel.sync.NamesBook
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The watch app's state for Compose: the shared [MatchSession] (setup included), plus the team names.
 * The stores open off the main thread; until then [state] is null and the app draws only its background.
 * The [phone] link mirrors every scoreboard to the phone and brings back the names typed there.
 */
class WatchViewModel(
    openMatch: suspend () -> TextStore,
    openNames: suspend () -> TextStore,
    private val phone: SyncChannel?,
    private val clock: () -> Long = ::currentTimeMs,
    scope: CoroutineScope = MainScope(),
) : ViewModel(scope) {

    private var session: MatchSession? = null
    private var summaryTimer: Job? = null

    var state: SessionState? by mutableStateOf(null)
        private set
    var names: TeamNames by mutableStateOf(TeamNames.DEFAULT)
        private set

    init {
        phone?.let(::addCloseable)
        viewModelScope.launch {
            val namesBook = NamesBook(openNames())
            val sink = phone?.let { link -> ScoreboardSink { link.send(SyncKeys.SCOREBOARD_PATH, it.encode()) } }
            val session = MatchSession(openMatch(), sink, clock)
            names = namesBook.names
            phone?.listen(SyncKeys.NAMES_PATH) { if (namesBook.receive(it)) names = namesBook.names }
            session.onChange = {
                state = session.state
                scheduleSummary()
            }
            this@WatchViewModel.session = session
            state = session.state
            session.announce()
            scheduleSummary()
        }
    }

    fun chooseSetup(index: Int): Feedback = session?.chooseSetup(index) ?: Feedback.NONE
    fun back(): Boolean = session?.back() ?: false

    /** Tests start a match directly; the app goes through [chooseSetup]. */
    @VisibleForTesting
    fun start(config: Config) {
        session?.start(config)
    }

    fun score(team: Team): Feedback = session?.score(team) ?: Feedback.NONE
    fun undo(): Feedback = session?.undo() ?: Feedback.NONE
    fun answerPrompt(choice: Int): Feedback = session?.answerPrompt(choice) ?: Feedback.NONE
    fun end() {
        session?.end()
    }
    fun reopen() {
        session?.reopen()
    }
    fun newMatch() {
        session?.newMatch()
    }

    /** Shows the final score briefly, then the summary; an undo in between cancels it. */
    private fun scheduleSummary() {
        if (state?.isAwaitingSummary != true) {
            summaryTimer?.cancel()
            summaryTimer = null
        } else if (summaryTimer == null) {
            summaryTimer = viewModelScope.launch {
                delay(UiTimings.AUTO_SUMMARY_DELAY_MS)
                summaryTimer = null
                end()
            }
        }
    }
}
