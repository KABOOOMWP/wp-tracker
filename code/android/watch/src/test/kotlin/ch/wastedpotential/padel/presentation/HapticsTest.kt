package ch.wastedpotential.padel.presentation

import ch.wastedpotential.padel.display.UiTimings.PULSE_BUZZ_MS
import ch.wastedpotential.padel.display.UiTimings.PULSE_GAP_MS
import ch.wastedpotential.padel.display.UiTimings.PULSE_LONG_MS
import ch.wastedpotential.padel.display.UiTimings.PULSE_TAP_MS
import ch.wastedpotential.padel.match.Feedback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What reaches the vibrator: one waveform per feedback, off/on pairs at full strength. */
class HapticsTest {

    private val played = mutableListOf<Pair<List<Long>, List<Int>>>()
    private val haptics = Haptics { timings, amplitudes -> played += timings.toList() to amplitudes.toList() }

    @Test fun `each feedback plays its own waveform`() {
        val expected = mapOf(
            Feedback.CONFIRM to listOf(0, PULSE_TAP_MS),
            Feedback.POINT_YOU to listOf(0, PULSE_BUZZ_MS),
            Feedback.POINT_OPP to listOf(0, PULSE_BUZZ_MS, PULSE_GAP_MS, PULSE_BUZZ_MS),
            Feedback.UNDO to listOf(0, PULSE_TAP_MS, PULSE_GAP_MS, PULSE_TAP_MS, PULSE_GAP_MS, PULSE_TAP_MS),
            Feedback.GAME_WON to listOf(0, PULSE_LONG_MS),
            Feedback.SIDE_CHANGE to listOf(0, PULSE_LONG_MS, PULSE_GAP_MS, PULSE_LONG_MS),
        )
        expected.forEach { (feedback, timings) ->
            played.clear()
            haptics.play(feedback)
            val (playedTimings, amplitudes) = played.single()
            assertEquals(timings.map(Number::toLong), playedTimings, "$feedback timings")
            assertEquals(timings.indices.map { if (it % 2 == 1) 255 else 0 }, amplitudes, "$feedback amplitudes")
        }
        assertEquals((Feedback.entries - Feedback.NONE).toSet(), expected.keys, "every feedback that buzzes is covered")
    }

    @Test fun `no feedback leaves the vibrator alone`() {
        haptics.play(Feedback.NONE)
        assertTrue(played.isEmpty())
    }
}
