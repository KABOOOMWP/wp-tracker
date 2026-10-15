package ch.wastedpotential.padel.sync

/** Where the watch ↔ phone payloads travel (values are JSON from this module). */
public object SyncKeys {
    /** WatchConnectivity dictionary keys (Apple). */
    public const val SCOREBOARD: String = "scoreboard"
    public const val NAMES: String = "names"

    /** Wearable Data Layer paths (Android). */
    public const val SCOREBOARD_PATH: String = "/scoreboard"
    public const val NAMES_PATH: String = "/names"
}
