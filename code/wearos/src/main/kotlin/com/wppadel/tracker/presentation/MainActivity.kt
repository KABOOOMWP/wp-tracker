package com.wppadel.tracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.wear.ambient.AmbientLifecycleObserver
import com.wppadel.tracker.haptic.HapticManager
import com.wppadel.tracker.model.Config
import com.wppadel.tracker.model.Snapshot
import com.wppadel.tracker.presentation.match.MatchScreen
import com.wppadel.tracker.presentation.setup.SetupScreen
import com.wppadel.tracker.presentation.summary.SummaryScreen
import com.wppadel.tracker.presentation.theme.LocalIsRoundScreen
import com.wppadel.tracker.presentation.theme.LocalWatchScale
import com.wppadel.tracker.presentation.theme.WEAR_SCALE_REF_DP

private sealed class AppScreen {
    object Setup      : AppScreen()
    data class Match(val config: Config)       : AppScreen()
    data class Summary(val snapshot: Snapshot) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private lateinit var haptic: HapticManager

    // Keeps the activity alive when the display goes ambient (dim/always-on),
    // rather than navigating back to the watch face. Works with keepActivity
    // metadata in AndroidManifest to survive display sleep during a match.
    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {}
            override fun onExitAmbient() {}
            override fun onUpdateAmbient() {}
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        haptic = HapticManager(this)
        setContent {
            // Compute scale once at the root from the actual screen width.
            // All child composables read LocalWatchScale.current for proportional sizing.
            val config = LocalConfiguration.current
            val screenWidthDp = config.screenWidthDp.toFloat()
            val scale   = screenWidthDp / WEAR_SCALE_REF_DP
            val isRound = config.isScreenRound
            CompositionLocalProvider(
                LocalWatchScale    provides scale,
                LocalIsRoundScreen provides isRound
            ) {
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
