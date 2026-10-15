package ch.wastedpotential.padel.display

/** Interaction timings both apps must agree on. */
public object UiTimings {
    /** Pause on the final score before the summary opens. */
    public const val AUTO_SUMMARY_DELAY_MS: Long = 400

    /** Hold time on the undo button to toggle it into the end-match button. */
    public const val END_MATCH_HOLD_MS: Long = 1_500

    /** Haptic pulse lengths: a light tap, a clear buzz and a long buzz. */
    public const val PULSE_TAP_MS: Long = 150
    public const val PULSE_BUZZ_MS: Long = 350
    public const val PULSE_LONG_MS: Long = 750

    /** Silence between pulses; long enough to count them, and for watchOS to play each one. */
    public const val PULSE_GAP_MS: Long = 450
}
