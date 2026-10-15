package ch.wastedpotential.padel.phone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.display.SummaryStats
import ch.wastedpotential.padel.link.SyncChannel
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.sync.NamesBook
import ch.wastedpotential.padel.sync.PhoneBoard
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** Everything the phone draws: the watch's match or summary (neither: the waiting board) and the team names. */
data class BoardState(val match: MatchDisplay?, val summary: SummaryStats?, val showsNameEditor: Boolean, val names: TeamNames) {
    companion object {
        fun of(board: PhoneBoard, names: TeamNames) = BoardState(board.match, board.summary, board.showsNameEditor, names)
    }
}

/**
 * The phone's state: the newest scoreboard from the [watch], and the team names edited here.
 * The names store opens off the main thread, then the watch link starts; the view model closes the link when cleared.
 * [sample] (a Scoreboard as JSON) is shown first; the debug sample boards and UI tests use it.
 */
class BoardViewModel(
    openNames: suspend () -> TextStore,
    private val watch: SyncChannel?,
    sample: String? = null,
    scope: CoroutineScope = MainScope(),
) : ViewModel(scope) {

    private val phoneBoard = PhoneBoard()
    private var namesBook: NamesBook? = null

    /** Null until the saved names are read, so the default names never flash. */
    var state: BoardState? by mutableStateOf(null)
        private set

    init {
        watch?.let(::addCloseable)
        sample?.let(phoneBoard::receive)
        viewModelScope.launch {
            val store = openNames()
            val book = NamesBook(store).also { namesBook = it }
            publish()
            watch?.listen(SyncKeys.SCOREBOARD_PATH) { if (phoneBoard.receive(it)) publish() }
            // Only names typed on this phone: after a reinstall its defaults would overwrite the ones on the watch
            if (store.load() != null) watch?.send(SyncKeys.NAMES_PATH, book.names.encode())
        }
    }

    /** Saves an edit from the name editor (a blank field keeps its name) and sends it to the watch. */
    fun editNames(you: String, opp: String) {
        val edited = namesBook?.edit(you, opp) ?: return
        publish()
        watch?.send(SyncKeys.NAMES_PATH, edited.encode())
    }

    private fun publish() {
        state = BoardState.of(phoneBoard, namesBook?.names ?: return)
    }
}
