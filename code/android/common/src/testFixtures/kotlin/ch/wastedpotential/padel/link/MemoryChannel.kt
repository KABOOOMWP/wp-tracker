package ch.wastedpotential.padel.link

/**
 * [SyncChannel] in memory for tests. Like the Data Layer it keeps the last value per path and
 * replays it to a new listener. [pair] links two channels the way a watch and a phone are linked.
 */
class MemoryChannel private constructor(private val items: MutableMap<String, String>) : SyncChannel {

    constructor() : this(mutableMapOf())

    val sent = mutableListOf<Pair<String, String>>()
    var isClosed = false
        private set
    private var peer: MemoryChannel? = null
    private val listeners = mutableMapOf<String, MutableList<(String) -> Unit>>()

    override fun send(path: String, data: String) {
        sent += path to data
        items[path] = data
        peer?.deliver(path, data)
    }

    override fun listen(path: String, onData: (String) -> Unit) {
        if (isClosed) return
        listeners.getOrPut(path) { mutableListOf() } += onData
        items[path]?.let(onData)
    }

    override fun close() {
        isClosed = true
        listeners.clear()
    }

    /** What the other device would deliver; kept for listeners that start later. */
    fun receive(path: String, data: String) {
        items[path] = data
        deliver(path, data)
    }

    fun lastSent(path: String): String? = sent.lastOrNull { it.first == path }?.second

    private fun deliver(path: String, data: String) = listeners[path].orEmpty().toList().forEach { it(data) }

    companion object {
        /** Two linked channels: what one sends, the other receives, now or when it starts listening. */
        fun pair(): Pair<MemoryChannel, MemoryChannel> {
            val items = mutableMapOf<String, String>()
            val a = MemoryChannel(items)
            val b = MemoryChannel(items)
            a.peer = b
            b.peer = a
            return a to b
        }
    }
}
