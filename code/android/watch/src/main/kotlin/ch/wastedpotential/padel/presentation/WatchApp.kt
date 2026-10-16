package ch.wastedpotential.padel.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import ch.wastedpotential.padel.board.LocalBoardScale
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.BoardMetrics
import ch.wastedpotential.padel.display.UiTimings
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.presentation.match.MatchScreen
import ch.wastedpotential.padel.presentation.setup.SetupView
import ch.wastedpotential.padel.presentation.summary.SummaryScreen

/** Round watches need wider side insets. */
val LocalIsRoundScreen = staticCompositionLocalOf { false }

/** The whole watch UI: the screen the session is on, each action wired to the view model and its feedback. */
@Composable
fun WatchApp(vm: WatchViewModel, haptics: Haptics, undoHoldMs: Long = UiTimings.END_MATCH_HOLD_MS) {
    val state = vm.state
    // Back steps through the setup and never leaves a running match (a wrist flick can send it)
    BackHandler(enabled = state?.handlesBack == true) { vm.back() }
    WatchFrame {
        when (state?.screen) {
            // the stores are still opening: background only, never a wrong screen
            null -> Unit

            AppScreen.SETUP -> state.setup?.let { SetupView(it) { i -> haptics.play(vm.chooseSetup(i)) } }

            AppScreen.MATCH -> state.display?.let { display ->
                MatchScreen(
                    display = display,
                    names = vm.names,
                    onScore = { team -> haptics.play(vm.score(team)) },
                    onUndo = { haptics.play(vm.undo()) },
                    onEndMatch = vm::end,
                    onAnswer = { i -> haptics.play(vm.answerPrompt(i)) },
                    holdMs = undoHoldMs,
                )
            }

            AppScreen.SUMMARY -> state.summary?.let { SummaryScreen(it, vm.names, vm::reopen, vm::newMatch) }
        }
    }
}

/** The previews' watch: a small round screen at the board's reference width. */
internal const val WATCH_PREVIEW = "spec:width=192dp,height=192dp,dpi=320,isRound=true"

/** The black watch screen with the board scaled to its width; the app and the previews draw inside it. */
@Composable
fun WatchFrame(content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(BoardColors.BACKGROUND))) {
        CompositionLocalProvider(
            LocalBoardScale provides maxWidth.value / BoardMetrics.WEAR_REFERENCE_WIDTH,
            LocalIsRoundScreen provides LocalConfiguration.current.isScreenRound,
        ) {
            content()
        }
    }
}
