package ch.wastedpotential.padel.storage

import androidx.core.util.AtomicFile
import ch.wastedpotential.padel.platform.TextStore
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** One background thread for every store in the app, so saves keep their order across files. */
private val diskThread: Executor = Executors.newSingleThreadExecutor { Thread(it, "text-store").apply { isDaemon = true } }

/**
 * One text file in app storage (the saved match, the team names). [open] reads it once on the disk thread;
 * after that [load] answers from memory and [save] queues the write, so the main thread never waits for the disk.
 */
class FileTextStore private constructor(private val file: AtomicFile, private val disk: Executor, private var text: String?) : TextStore {

    override fun load(): String? = text

    override fun save(data: String) {
        text = data
        disk.execute {
            val out = try {
                file.startWrite()
            } catch (_: IOException) {
                return@execute
            }
            try {
                out.write(data.encodeToByteArray())
                file.finishWrite(out)
            } catch (_: IOException) {
                file.failWrite(out)
            }
        }
    }

    companion object {
        /** Reads [file] on [disk] after every write queued before, so a new store never reads stale text. */
        suspend fun open(file: File, disk: Executor = diskThread): FileTextStore {
            val atomic = AtomicFile(file)
            val text = withContext(disk.asCoroutineDispatcher()) {
                try {
                    atomic.readFully().decodeToString()
                } catch (_: IOException) {
                    null
                }
            }
            return FileTextStore(atomic, disk, text)
        }
    }
}
