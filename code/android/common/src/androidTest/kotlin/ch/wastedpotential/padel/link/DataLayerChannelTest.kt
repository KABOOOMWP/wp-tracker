package ch.wastedpotential.padel.link

import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import org.junit.Assume.assumeTrue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * The real Wear Data Layer on one device (needs Google Play services). The Data Layer only reports
 * changes from the other device live, so this checks what one device can: the stored item reaches
 * a listener that starts later, and a closed channel stays silent. Watch to phone is checked by hand.
 */
class DataLayerChannelTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val path = "/test/${System.nanoTime()}"
    private val channels = mutableListOf<DataLayerChannel>()

    private fun channel() = DataLayerChannel(instrumentation.targetContext).also(channels::add)

    /** Phones without the Wear OS (or Galaxy Wearable) app have no Data Layer: nothing to test there. */
    @BeforeTest fun needsTheDataLayer() {
        val client = Wearable.getDataClient(instrumentation.targetContext)
        val available = runCatching {
            Tasks.await(GoogleApiAvailability.getInstance().checkApiAvailability(client), 10, TimeUnit.SECONDS)
        }.isSuccess
        assumeTrue("The Wear Data Layer is unavailable on this device", available)
    }

    @AfterTest fun closeAll() = channels.forEach(DataLayerChannel::close)

    /** Listens with a new channel until the stored value arrives (a put takes a moment). */
    private fun readStored(): String? {
        repeat(15) {
            val received = LinkedBlockingQueue<String>()
            channel().listen(path) { received += it }
            received.poll(1, TimeUnit.SECONDS)?.let { return it }
        }
        return null
    }

    @Test fun aLaterListenerGetsTheCurrentValue() {
        channel().send(path, "first")
        assertEquals("first", readStored())
        channel().send(path, "second")
        repeat(15) { if (readStored() == "second") return }
        fail("the newer value never replaced the stored one")
    }

    @Test fun aClosedChannelDeliversNothing() {
        channel().send(path, "stored")
        assertEquals("stored", readStored())

        val received = LinkedBlockingQueue<String>()
        // On the main thread, so the stored item cannot arrive between listen and close
        instrumentation.runOnMainSync {
            channel().apply {
                listen(path) { received += it }
                close()
            }
        }
        assertNull(received.poll(3, TimeUnit.SECONDS))
    }

    @Test fun listeningAfterCloseDeliversNothing() {
        channel().send(path, "stored")
        assertEquals("stored", readStored())

        val received = LinkedBlockingQueue<String>()
        instrumentation.runOnMainSync {
            channel().apply {
                close()
                listen(path) { received += it }
            }
        }
        assertNull(received.poll(3, TimeUnit.SECONDS))
    }
}
