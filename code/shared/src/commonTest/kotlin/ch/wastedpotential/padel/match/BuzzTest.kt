package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.match.Pulse.BUZZ
import ch.wastedpotential.padel.match.Pulse.LONG_BUZZ
import ch.wastedpotential.padel.match.Pulse.TAP
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuzzTest {

    private val patterns = Feedback.entries.mapNotNull { f -> Buzz.of(f)?.let { f to it } }.toMap()

    @Test fun `every feedback has its rhythm`() {
        assertEquals(listOf(TAP), Buzz.of(Feedback.CONFIRM)?.pulses)
        assertEquals(listOf(BUZZ), Buzz.of(Feedback.POINT_YOU)?.pulses)
        assertEquals(listOf(BUZZ, BUZZ), Buzz.of(Feedback.POINT_OPP)?.pulses)
        assertEquals(listOf(TAP, TAP, TAP), Buzz.of(Feedback.UNDO)?.pulses)
        assertEquals(listOf(LONG_BUZZ), Buzz.of(Feedback.GAME_WON)?.pulses)
        assertEquals(listOf(LONG_BUZZ, LONG_BUZZ), Buzz.of(Feedback.SIDE_CHANGE)?.pulses)
        assertNull(Buzz.of(Feedback.NONE))
    }

    @Test fun `no two feedbacks feel the same`() {
        assertEquals(patterns.size, patterns.values.toSet().size)
    }

    @Test fun `pulse kinds are clearly apart in length`() {
        assertTrue(TAP.ms * 2 <= BUZZ.ms)
        assertTrue(BUZZ.ms * 2 <= LONG_BUZZ.ms)
    }

    @Test fun `pulses are far enough apart to count and for watchOS to play them all`() {
        assertTrue(UiTimings.PULSE_GAP_MS >= 400)
        patterns.values.forEach { buzz ->
            buzz.startsMs.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= 600, "$buzz starts too close") }
        }
    }

    @Test fun `every pattern ends within two seconds`() {
        patterns.values.forEach { assertTrue(it.durationMs <= 2_000, "$it lasts ${it.durationMs} ms") }
    }

    @Test fun `starts add each pulse and one gap`() {
        assertEquals(listOf(0L, 600L, 1200L), Buzz(listOf(TAP, TAP, TAP)).startsMs)
        assertEquals(listOf(0L, 1200L), Buzz(listOf(LONG_BUZZ, LONG_BUZZ)).startsMs)
        assertEquals(1950L, Buzz(listOf(LONG_BUZZ, LONG_BUZZ)).durationMs)
    }

    @Test fun `waveform alternates silence and pulse and starts at once`() {
        val buzz = Buzz(listOf(BUZZ, TAP))
        assertContentEquals(longArrayOf(0, 350, 450, 150), buzz.timings)
        assertContentEquals(intArrayOf(0, 255, 0, 255), buzz.amplitudes)
    }
}
