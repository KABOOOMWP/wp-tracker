package com.wppadel.tracker.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object WPColors {
    val Background      = Color(0xFF000000)
    val YouPanel        = Color(0xFF0B1D36)   // dark navy
    val OppPanel        = Color(0xFF421F00)
    val YouAccent       = Color(0xFF4A9EF8)   // blue
    val OppAccent       = Color(0xFFFF9500)   // orange
    val Divider         = Color(0xFFFFFFFF)
    val ScoreText       = Color(0xFFFFFFFF)
    val PillBg          = Color(0xFFD4A017)   // golden yellow
    val PillText        = Color(0xFF000000)
    val UndoBg          = Color(0xFF383838)   // dark grey
}

// Reference screen width in dp (Pixel Watch 2 / Galaxy Watch 4 40 mm ≈ 192 dp).
// watchScale = screenWidthDp / 192.  All size constants below are tuned for scale = 1.
const val WEAR_SCALE_REF_DP = 192f

// Provided once at the root of the composition tree (MainActivity).
// Every composable can read LocalWatchScale.current to get the scale factor.
val LocalWatchScale    = staticCompositionLocalOf { 1f }
val LocalIsRoundScreen = staticCompositionLocalOf { false }
