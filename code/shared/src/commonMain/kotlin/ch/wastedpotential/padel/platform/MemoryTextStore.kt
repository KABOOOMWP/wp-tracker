package ch.wastedpotential.padel.platform

/** Keeps the text in memory only: for tests and the apps' UI-test launch modes. */
public class MemoryTextStore(private var data: String? = null) : TextStore {
    override fun load(): String? = data
    override fun save(data: String) {
        this.data = data
    }
}
