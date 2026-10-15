package ch.wastedpotential.padel.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ch.wastedpotential.padel.match.Buzz
import ch.wastedpotential.padel.match.Feedback

/** Plays the shared [Buzz] of every action's [Feedback] as one waveform: off/on [timings] in ms with their [amplitudes]. */
class Haptics(private val vibrate: (timings: LongArray, amplitudes: IntArray) -> Unit) {

    fun play(feedback: Feedback) {
        val buzz = Buzz.of(feedback) ?: return
        vibrate(buzz.timings, buzz.amplitudes)
    }

    companion object {
        /** The watch's vibrator; a new vibration replaces one still playing, so quick taps never blur together. */
        fun of(context: Context): Haptics {
            val vibrator: Vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
            return Haptics { timings, amplitudes ->
                if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        }
    }
}
