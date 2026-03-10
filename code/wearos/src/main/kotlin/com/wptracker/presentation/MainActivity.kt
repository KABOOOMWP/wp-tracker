package com.wptracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import com.wptracker.haptic.HapticManager
import com.wptracker.model.Config
import com.wptracker.model.Snapshot
import com.wptracker.presentation.match.MatchScreen
import com.wptracker.presentation.setup.SetupScreen
import com.wptracker.presentation.summary.SummaryScreen
import com.wptracker.presentation.theme.LocalWatchScale
import com.wptracker.presentation.theme.WEAR_SCALE_REF_DP

private sealed class AppScreen {
    object Setup   : AppScreen()
    data class Match(val config: Config)       : AppScreen()
    data class Summary(val snapshot: Snapshot) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private lateinit var haptic: HapticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        haptic = HapticManager(this)
        setContent {
            // Compute scale once at the root from the actual screen width.
            // All child composables read LocalWatchScale.current for proportional sizing.
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
            val scale = screenWidthDp / WEAR_SCALE_REF_DP
            CompositionLocalProvider(LocalWatchScale provides scale) {
                WPTrackerApp(haptic)
            }
        }
    }
}

@Composable
private fun WPTrackerApp(haptic: HapticManager) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Setup) }

    when (val s = screen) {
        is AppScreen.Setup   -> SetupScreen(
            onMatchStart = { config -> screen = AppScreen.Match(config) }
        )
        is AppScreen.Match   -> MatchScreen(
            config     = s.config,
            haptic     = haptic,
            onMatchEnd = { snapshot -> screen = AppScreen.Summary(snapshot) }
        )
        is AppScreen.Summary -> SummaryScreen(
            snapshot   = s.snapshot,
            onNewMatch = { screen = AppScreen.Setup }
        )
    }
}
