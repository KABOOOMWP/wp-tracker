package ch.wastedpotential.padel.phone

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.wastedpotential.padel.link.MemoryChannel
import ch.wastedpotential.padel.platform.MemoryTextStore
import ch.wastedpotential.padel.platform.TextStore
import ch.wastedpotential.padel.sync.SampleBoards
import ch.wastedpotential.padel.sync.SyncKeys
import ch.wastedpotential.padel.sync.TeamNames
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {

    private val store = MemoryTextStore()
    private val watch = MemoryChannel()

    // The names open at once on this dispatcher
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private fun vm(sample: String? = null, link: MemoryChannel? = watch, openNames: suspend () -> TextStore = { store }) =
        BoardViewModel(openNames, link, sample, scope)

    private val BoardViewModel.shown: BoardState get() = assertNotNull(state, "the names are open")

    private fun board(revision: Long, sample: String = SampleBoards.MATCH, epoch: Long = 1) =
        SampleBoards.make(sample)!!.copy(epoch = epoch, revision = revision).encode()

    @Test fun `nothing is drawn until the saved names are read`() {
        store.save(TeamNames("Ana", "Bo").encode())
        val opening = CompletableDeferred<TextStore>()
        val vm = vm(openNames = { opening.await() })
        assertNull(vm.state, "the default names never flash")
        vm.editNames("Cy", "Di")
        opening.complete(store)
        assertEquals(TeamNames("Ana", "Bo"), vm.shown.names, "an edit before opening is dropped")
    }

    @Test fun `waits for the watch without a board`() {
        val shown = vm().shown
        assertNull(shown.match)
        assertNull(shown.summary)
        assertTrue(shown.showsNameEditor)
    }

    @Test fun `shows scoreboards from the watch`() {
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(5))
        assertEquals("40", vm.shown.match!!.youScore)
    }

    @Test fun `keeps the newest scoreboard by revision when they arrive out of order`() {
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(10, SampleBoards.SUMMARY))
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(5))
        assertNotNull(vm.shown.summary)
        assertNull(vm.shown.match)
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(11))
        assertNotNull(vm.shown.match)
    }

    @Test fun `follows a reinstalled watch although its revisions start over`() {
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(20, SampleBoards.SUMMARY, epoch = 1))
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(1, epoch = 2))
        assertNotNull(vm.shown.match)
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(0, SampleBoards.SUMMARY, epoch = 2))
        assertNotNull(vm.shown.match, "ordered by revision within the new epoch")
    }

    @Test fun `ignores unreadable scoreboards`() {
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, board(5))
        watch.receive(SyncKeys.SCOREBOARD_PATH, "garbage")
        assertEquals("40", vm.shown.match!!.youScore)
    }

    @Test fun `a sample board is shown first`() {
        val vm = vm(SampleBoards.make(SampleBoards.SUMMARY)!!.encode())
        assertNotNull(vm.shown.summary)
    }

    @Test fun `a fresh install sends no names, so the watch keeps its own`() {
        vm()
        assertNull(watch.lastSent(SyncKeys.NAMES_PATH))
    }

    @Test fun `names saved on this phone are sent on launch`() {
        store.save(TeamNames("Ana", "Bo").encode())
        vm()
        assertEquals(TeamNames("Ana", "Bo"), TeamNames.decode(watch.lastSent(SyncKeys.NAMES_PATH)!!))
    }

    @Test fun `edited names are shown, saved and sent`() {
        val vm = vm()
        vm.editNames(" Ana ", "")
        val expected = TeamNames("Ana", TeamNames.DEFAULT.opp)
        assertEquals(expected, vm.shown.names)
        assertEquals(expected, TeamNames.decode(watch.lastSent(SyncKeys.NAMES_PATH)!!))
        assertEquals(expected, vm(link = null).shown.names)
    }

    @Test fun `an edit that changes nothing is not sent`() {
        vm().editNames("", "  ")
        assertNull(watch.lastSent(SyncKeys.NAMES_PATH))
    }

    @Test fun `works without a watch link`() {
        val vm = vm(link = null)
        vm.editNames("Ana", "Bo")
        assertEquals(TeamNames("Ana", "Bo"), vm.shown.names)
        assertTrue(watch.sent.isEmpty())
    }

    @Test fun `shows the board the watch sent before the phone app started`() {
        val (watchSide, phoneSide) = MemoryChannel.pair()
        watchSide.send(SyncKeys.SCOREBOARD_PATH, board(5))
        assertEquals("40", vm(link = phoneSide).shown.match!!.youScore)
    }

    @Test fun `what to draw follows the board`() {
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, SampleBoards.make(SampleBoards.MATCH)!!.encode())
        assertNotNull(vm.shown.match)
        assertTrue(vm.shown.showsNameEditor)
        watch.receive(SyncKeys.SCOREBOARD_PATH, SampleBoards.make(SampleBoards.SUMMARY)!!.encode())
        assertNull(vm.shown.match)
        assertEquals(SampleBoards.make(SampleBoards.SUMMARY)!!.summary, vm.shown.summary)
        assertFalse(vm.shown.showsNameEditor, "the pencil would cover the stats")
    }

    @Test fun `clearing the view model closes the watch link`() {
        val viewModels = ViewModelStore()
        ViewModelProvider.create(viewModels, viewModelFactory { initializer { vm() } })[BoardViewModel::class]
        assertFalse(watch.isClosed)
        viewModels.clear()
        assertTrue(watch.isClosed)
    }

    @Test fun `the board drawn is exactly the one the watch encoded`() {
        val sample = SampleBoards.make(SampleBoards.MATCH)!!
        val vm = vm()
        watch.receive(SyncKeys.SCOREBOARD_PATH, sample.encode())
        assertEquals(sample.match, vm.shown.match)
    }
}
