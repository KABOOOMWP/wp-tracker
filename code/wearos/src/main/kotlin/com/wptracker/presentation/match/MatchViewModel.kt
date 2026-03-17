package com.wptracker.presentation.match

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.wptracker.engine.MatchEngine
import com.wptracker.model.*

class MatchViewModel : ViewModel() {

    private val _history = mutableStateListOf<Snapshot>()

    val current: Snapshot? get() = _history.lastOrNull()
    val canUndo: Boolean   get() = _history.size > 1

    /** Called once per match; safe to call again with a new config (resets state). */
    fun init(config: Config) {
        _history.clear()
        _history += MatchEngine.initialSnapshot(config, System.currentTimeMillis())
    }

    fun score(team: Team) {
        val snap = current ?: return
        val next = MatchEngine.score(snap, team)
        if (next !== snap) _history += next
    }

    fun undo() {
        if (canUndo) _history.removeLast()
    }

    fun setDeciderSide(side: ServeSide) {
        val snap    = current ?: return
        val updated = MatchEngine.setDeciderSide(snap, side)
        if (updated !== snap) _history[_history.lastIndex] = updated
    }

    fun pickOpponentFirstServer(player: Player) {
        val snap = current ?: return
        val updated = MatchEngine.pickOpponentFirstServer(snap, player)
        _history += updated  // append so undo shows picker again
    }

    fun confirmYouPositionSwitch(doSwitch: Boolean) {
        val snap = current ?: return
        val updated = MatchEngine.confirmYouPositionSwitch(snap, doSwitch)
        if (updated !== snap) _history[_history.lastIndex] = updated  // replace so undo skips overlay
    }

    fun confirmOppPositionSwitch(doSwitch: Boolean) {
        val snap = current ?: return
        val updated = MatchEngine.confirmOppPositionSwitch(snap, doSwitch)
        if (updated !== snap) _history[_history.lastIndex] = updated  // replace so undo skips overlay
    }
}
