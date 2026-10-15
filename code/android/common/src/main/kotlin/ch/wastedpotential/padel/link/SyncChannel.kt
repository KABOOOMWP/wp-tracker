package ch.wastedpotential.padel.link

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable

/**
 * The text link between the watch and the phone app, one value per path (see SyncKeys).
 * The latest value of a path persists, so the other device gets it when it connects later.
 */
interface SyncChannel : AutoCloseable {
    fun send(path: String, data: String)

    /** Calls [onData] on the main thread with the current value of [path] (if any) and every change after. */
    fun listen(path: String, onData: (String) -> Unit)

    /** Stops every listener; nothing is delivered after it, and a later [listen] does nothing. */
    override fun close()
}

/** [SyncChannel] over the Wear Data Layer. Without a paired device every call quietly does nothing. */
class DataLayerChannel(context: Context) : SyncChannel {

    private val client = Wearable.getDataClient(context.applicationContext)
    private val listeners = Subscriptions<DataClient.OnDataChangedListener>(client::removeListener)

    override fun send(path: String, data: String) {
        client.putDataItem(PutDataRequest.create(path).setData(data.encodeToByteArray()).setUrgent())
    }

    override fun listen(path: String, onData: (String) -> Unit) {
        // wear://*/path: the item of this path from every node, the other device's included
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority("*").path(path).build()
        val gate = SnapshotGate { if (listeners.isOpen) onData(it) }
        fun text(item: DataItem) = item.data?.decodeToString()

        val listener = DataClient.OnDataChangedListener { events ->
            events.filter { it.type == DataEvent.TYPE_CHANGED }.forEach { event -> text(event.dataItem)?.let(gate::live) }
        }
        if (!listeners.add(listener)) return
        client.addListener(listener, uri, DataClient.FILTER_LITERAL)
        client.getDataItems(uri, DataClient.FILTER_LITERAL).addOnSuccessListener { items ->
            items.forEach { item -> text(item)?.let(gate::snapshot) }
            items.release()
        }
    }

    override fun close() = listeners.close()
}

/** A channel's listeners: none is taken once closed, and closing removes each one exactly once. */
internal class Subscriptions<L>(private val remove: (L) -> Unit) {
    private val active = mutableListOf<L>()
    var isOpen = true
        private set

    /** False after [close]: the caller must not register [listener] anywhere. */
    fun add(listener: L): Boolean {
        if (isOpen) active += listener
        return isOpen
    }

    fun close() {
        isOpen = false
        active.forEach(remove)
        active.clear()
    }
}

/**
 * Orders a listener's two sources: the stored item read at start and the live changes.
 * Both arrive on the main thread; a stored item that comes after a live change is older, so it is dropped.
 */
internal class SnapshotGate(private val onData: (String) -> Unit) {
    private var gotLive = false

    fun live(data: String) {
        gotLive = true
        onData(data)
    }

    fun snapshot(data: String) {
        if (!gotLive) onData(data)
    }
}
