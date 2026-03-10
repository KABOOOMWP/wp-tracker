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

    fun pointYou() = vibrate(longArrayOf(0, 80))
    fun pointOpp() = vibrate(longArrayOf(0, 80, 100, 80))
    fun undo()     = vibrate(longArrayOf(0, 80, 100, 80, 100, 80))
    fun gameWin()  = vibrate(longArrayOf(0, 400))

    private fun vibrate(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
