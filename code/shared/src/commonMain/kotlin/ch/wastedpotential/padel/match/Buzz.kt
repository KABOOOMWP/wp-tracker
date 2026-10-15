package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.display.UiTimings

/** One vibration pulse; Android buzzes for [ms], watchOS plays its closest system haptic. */
public enum class Pulse(public val ms: Long) {
    TAP(UiTimings.PULSE_TAP_MS),
    BUZZ(UiTimings.PULSE_BUZZ_MS),
    LONG_BUZZ(UiTimings.PULSE_LONG_MS),
}

/** A vibration: [pulses] in order, UiTimings.PULSE_GAP_MS of silence between them. */
public data class Buzz(val pulses: List<Pulse>) {

    /** When each pulse starts, counted from the first (for platforms that play one haptic at a time). */
    public val startsMs: List<Long> get() = pulses.runningFold(0L) { start, p -> start + p.ms + UiTimings.PULSE_GAP_MS }.dropLast(1)

    /** From the first pulse's start to the last pulse's end. */
    public val durationMs: Long get() = startsMs.last() + pulses.last().ms

    /** Waveform timings: off/on pairs, starting without a delay. */
    public val timings: LongArray get() = LongArray(pulses.size * 2) { i ->
        when {
            i % 2 == 1 -> pulses[i / 2].ms
            i == 0 -> 0
            else -> UiTimings.PULSE_GAP_MS
        }
    }

    /** Waveform amplitudes matching [timings]: full strength on, silent off. */
    public val amplitudes: IntArray get() = IntArray(pulses.size * 2) { i -> if (i % 2 == 1) 255 else 0 }

    public companion object {
        /** A rhythm per meaning: points buzz (you 1, opponent 2), undo ticks 3 times, answers tap once, game and side change buzz long. */
        public fun of(feedback: Feedback): Buzz? = when (feedback) {
            Feedback.CONFIRM -> Buzz(listOf(Pulse.TAP))
            Feedback.POINT_YOU -> Buzz(listOf(Pulse.BUZZ))
            Feedback.POINT_OPP -> Buzz(listOf(Pulse.BUZZ, Pulse.BUZZ))
            Feedback.UNDO -> Buzz(listOf(Pulse.TAP, Pulse.TAP, Pulse.TAP))
            Feedback.GAME_WON -> Buzz(listOf(Pulse.LONG_BUZZ))
            Feedback.SIDE_CHANGE -> Buzz(listOf(Pulse.LONG_BUZZ, Pulse.LONG_BUZZ))
            Feedback.NONE -> null
        }
    }
}
