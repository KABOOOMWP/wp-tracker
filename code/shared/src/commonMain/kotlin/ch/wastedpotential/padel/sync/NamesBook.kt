package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.platform.TextStore

/** The team names on one device, kept in [store]: edited on the phone, received on the watch. */
public class NamesBook(private val store: TextStore) {

    public var names: TeamNames = store.load()?.let(TeamNames::decode) ?: TeamNames.DEFAULT
        private set

    /** Applies a name-editor edit (blank keeps a name); returns the names to send, or null if unchanged. */
    public fun edit(you: String, opp: String): TeamNames? = names.edited(you, opp).takeIf(::update)

    /** Receives names from the other device; unreadable data is ignored. Returns true when they changed. */
    public fun receive(data: String): Boolean = TeamNames.decode(data)?.let(::update) ?: false

    private fun update(incoming: TeamNames): Boolean {
        if (incoming == names) return false
        names = incoming
        store.save(incoming.encode())
        return true
    }
}
