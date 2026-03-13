package com.wptracker.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Maps the four haptic events from the spec:
 *   pointYou  → 1× short (80 ms)
 *   pointOpp  → 2× short (80 ms, gap 100 ms, 80 ms)
 *   undo      → 3× short
 *   gameWin   → 1× long (400 ms)  — used for game / set / match win
 */
class HapticManager(context: Context) {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator =
        context.getSystemService(Vibrator::class.java)

    fun pointYou() = vibrate(longArrayOf(0, 100))
    fun pointOpp() = vibrate(longArrayOf(0, 100, 120, 100))
    fun undo()     = vibrate(longArrayOf(0, 100, 120, 100, 120, 100))
    fun gameWin()  = vibrate(longArrayOf(0, 500))

    private fun vibrate(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Pattern alternates: silence, buzz, silence, buzz, ...
            // Even indices are always silent (delay/gap), odd indices are always buzzes.
            val amplitudes = IntArray(pattern.size) { i -> if (i % 2 == 1) 255 else 0 }
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
