package ch.wastedpotential.padel.platform

import kotlin.time.Clock

/** Wall-clock time in milliseconds since 1970. */
public fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

/** A small text file (or preference) each app provides: the saved match, the team names. */
public interface TextStore {
    public fun load(): String?
    public fun save(data: String)
}
