package ch.wastedpotential.padel.storage

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileTextStoreTest {

    private val dir = Files.createTempDirectory("store").toFile()
    private val file = File(dir, "match.json")

    /** A disk thread under the test's control: runs work at once, or holds it while [held]. */
    private class TestDisk : Executor {
        var held = false
        private val queue = ArrayDeque<Runnable>()
        override fun execute(task: Runnable) = if (held) queue.addLast(task) else task.run()
        fun release() {
            held = false
            while (queue.isNotEmpty()) queue.removeFirst().run()
        }
    }

    private val disk = TestDisk()
    private suspend fun open(at: File = file) = FileTextStore.open(at, disk)

    @AfterTest fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test fun `a missing file loads as null`() = runTest {
        assertNull(open().load())
    }

    @Test fun `saved text survives a new store`() = runTest {
        open().save("{\"a\":1}")
        assertEquals("{\"a\":1}", open().load())
    }

    @Test fun `a save replaces the previous text`() = runTest {
        val store = open()
        store.save("first")
        store.save("second")
        assertEquals("second", store.load())
        assertEquals("second", file.readText())
    }

    @Test fun `after opening, load and save never wait for the disk`() = runTest {
        file.writeText("on disk")
        val store = open()
        disk.held = true
        assertEquals("on disk", store.load())
        store.save("new")
        assertEquals("new", store.load())
        assertEquals("on disk", file.readText(), "the write is only queued")
        disk.release()
        assertEquals("new", file.readText())
    }

    @Test fun `with the app's disk thread a new store reads what an earlier one queued`() = runTest {
        val first = FileTextStore.open(file)
        repeat(50) { first.save("text $it") }
        assertEquals("text 49", FileTextStore.open(file).load())
    }

    @Test fun `an unwritable location keeps the old text and does not throw`() = runTest {
        file.writeText("plain file")
        val inside = File(file, "inside-a-file.json")
        open(inside).save("lost")
        assertEquals("plain file", file.readText())
        assertNull(open(inside).load())
    }
}
