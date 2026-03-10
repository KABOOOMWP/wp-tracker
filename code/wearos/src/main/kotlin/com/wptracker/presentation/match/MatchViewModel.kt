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
        if (_history.isNotEmpty() && _history.first().config == config) return
        _history.clear()
        val first = config.serveOrder.first()
        _history += Snapshot(
            config = config,
            match  = MatchState(startedAt = System.currentTimeMillis()),
            set    = SetState(),
            game   = GameState(),
            serve  = ServeState(
                serverTeam       = first.team(),
                serverPlayer     = first,
                serveSide        = ServeSide.RIGHT,
                serveOrderIndex  = 0
            ),
            stats  = StatsState()
        )
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
}
