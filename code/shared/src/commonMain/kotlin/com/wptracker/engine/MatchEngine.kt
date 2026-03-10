package com.wptracker.engine

import com.wptracker.model.*

/**
 * Pure, stateless scoring engine.
 *
 * Single entry point: [score] takes the current [Snapshot] and the team that just won a point,
 * and returns a new [Snapshot]. The caller maintains the history stack (List<Snapshot>).
 * Undo = pop the last element.
 *
 * [computePill] derives the status-pill state from a snapshot (for UI rendering).
 */
object MatchEngine {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Diagonal layout of the two score numbers on screen.
     *
     * Rule: even rallyIndex → You bottom-right / Opp top-left.
     *       odd  rallyIndex → You bottom-left / Opp top-right.
     * rallyIndex = youPoints + oppPoints in the current game (resets to 0 on new game).
     * Applies identically to regular games, deuce/advantage states, and tie-break.
     */
    fun computeScoreLayout(snapshot: Snapshot): ScoreLayout {
        val rallyIndex = snapshot.game.youPoints + snapshot.game.oppPoints
        return if (rallyIndex % 2 == 0) {
            ScoreLayout(ScorePosition.BOTTOM_RIGHT, ScorePosition.TOP_LEFT)
        } else {
            ScoreLayout(ScorePosition.BOTTOM_LEFT, ScorePosition.TOP_RIGHT)
        }
    }

    /**
     * Receiver-side override for Golden or Star Point deciding points (spec §9.2, §9.3).
     *
     * At 40:40 in Golden/Star mode the receiving team may choose their return side (left/right)
     * and may not switch positions for that one point.
     *
     * This stores the choice in [GameState.deciderReceiveSideOverride] and updates
     * [ServeState.serveSide] so the white-dot moves to the chosen side immediately.
     * The override clears automatically when the deciding point is played (new game resets).
     *
     * No-op outside [GamePhase.GOLDEN] and [GamePhase.STAR_POINT].
     */
    fun setDeciderSide(snapshot: Snapshot, side: ServeSide): Snapshot {
        val phase = snapshot.game.phase
        if (phase != GamePhase.GOLDEN && phase != GamePhase.STAR_POINT) return snapshot
        return snapshot.copy(
            game = snapshot.game.copy(deciderReceiveSideOverride = side),
            serve = snapshot.serve.copy(serveSide = side)
        )
    }

    /**
     * Creates the first [Snapshot] for a new match.
     * Exposed so Swift callers avoid constructing Kotlin data classes directly.
     */
    fun initialSnapshot(config: Config, startedAt: Long): Snapshot {
        val first = config.serveOrder.first()
        return Snapshot(
            config = config,
            match  = MatchState(startedAt = startedAt),
            set    = SetState(),
            game   = GameState(),
            serve  = ServeState(
                serverTeam      = first.team(),
                serverPlayer    = first,
                serveSide       = ServeSide.RIGHT,
                serveOrderIndex = 0
            ),
            stats  = StatsState()
        )
    }

    /** Score a point for [team]. Returns the same snapshot if the match is already over. */
    fun score(snapshot: Snapshot, team: Team): Snapshot {
        if (snapshot.isMatchOver) return snapshot
        return applyPoint(snapshot, team)
    }

    /**
     * Compute the status pill for the given snapshot.
     *
     * Priority (highest first): MATCH_POINT > SET_POINT > GOLDEN_POINT > STAR_POINT
     *   > BREAK_POINT > TIEBREAK > HIDDEN
     *
     * Golden/Star deciding phases outrank Break because the deciding-point label is more
     * meaningful than the break label in those situations. Break is only shown in normal
     * play (e.g. 30:40, ADV_OPP while serving).
     */
    fun computePill(snapshot: Snapshot): PillState {
        val game = snapshot.game
        val serve = snapshot.serve
        val set = snapshot.set
        val match = snapshot.match
        val config = snapshot.config

        val isTieBreak = game.mode == GameMode.TIEBREAK

        // Determine if either team has a game-deciding next point
        val youHasGamePoint = canWinGameNextPoint(game, Team.YOU)
        val oppHasGamePoint = canWinGameNextPoint(game, Team.OPP)

        // Determine if winning this game wins the set for the respective team
        val youHasSetPoint = youHasGamePoint && canWinSetNextGame(set, match, Team.YOU)
        val oppHasSetPoint = oppHasGamePoint && canWinSetNextGame(set, match, Team.OPP)

        // Determine if winning this set wins the match
        val setsToWin = (config.bestOf + 1) / 2
        val youHasMatchPoint = youHasSetPoint && (match.setsWonYou + 1 >= setsToWin)
        val oppHasMatchPoint = oppHasSetPoint && (match.setsWonOpp + 1 >= setsToWin)

        // Break point: the returning team (non-server) has a game point
        val youIsReturning = serve.serverTeam == Team.OPP
        val oppIsReturning = serve.serverTeam == Team.YOU
        val breakPoint = (youIsReturning && youHasGamePoint) || (oppIsReturning && oppHasGamePoint)

        return when {
            youHasMatchPoint || oppHasMatchPoint -> PillState.MATCH_POINT
            youHasSetPoint || oppHasSetPoint     -> PillState.SET_POINT
            game.phase == GamePhase.GOLDEN       -> PillState.GOLDEN_POINT
            game.phase == GamePhase.STAR_POINT   -> PillState.STAR_POINT
            breakPoint                           -> PillState.BREAK_POINT
            isTieBreak                           -> PillState.TIEBREAK
            else                                 -> PillState.HIDDEN
        }
    }

    // -------------------------------------------------------------------------
    // Point application
    // -------------------------------------------------------------------------

    private fun applyPoint(snapshot: Snapshot, team: Team): Snapshot {
        val newYou = if (team == Team.YOU) snapshot.game.youPoints + 1 else snapshot.game.youPoints
        val newOpp = if (team == Team.OPP) snapshot.game.oppPoints + 1 else snapshot.game.oppPoints
        val newStats = snapshot.stats.copy(
            totalPlayedPoints = snapshot.stats.totalPlayedPoints + 1,
            pointsWonYou = snapshot.stats.pointsWonYou + if (team == Team.YOU) 1 else 0
        )

        val winner = gameWinner(snapshot.game, snapshot.config.ruleMode, team, newYou, newOpp)

        return if (winner != null) {
            // Track golden decider stats before applying game win
            val stats = if (snapshot.game.phase == GamePhase.GOLDEN) {
                newStats.copy(
                    goldenDecidersPlayed = newStats.goldenDecidersPlayed + 1,
                    goldenDecidersWonYou = newStats.goldenDecidersWonYou + if (winner == Team.YOU) 1 else 0
                )
            } else newStats
            applyGameWin(snapshot, winner, stats)
        } else {
            val (newPhase, newAdvCount) = nextGamePhase(
                snapshot.game, snapshot.config.ruleMode, team, newYou, newOpp
            )
            // Count every transition into deuce (first deuce and returns after advantage)
            val statsAfterDeuce = if (newPhase == GamePhase.DEUCE) {
                newStats.copy(deuceCount = newStats.deuceCount + 1)
            } else newStats
            val newGame = snapshot.game.copy(
                youPoints = newYou,
                oppPoints = newOpp,
                phase = newPhase,
                starAdvCount = newAdvCount
            )
            // Tie-break: update full serve state (player rotates per TB rotation formula)
            // Regular: only the serve side flips, server player stays constant within a game
            val newServe = if (snapshot.game.mode == GameMode.TIEBREAK) {
                tieBreakServe(snapshot, newYou, newOpp)
            } else {
                snapshot.serve.copy(serveSide = serveSideFromRallyIndex(newYou + newOpp))
            }
            snapshot.copy(game = newGame, serve = newServe, stats = statsAfterDeuce)
        }
    }

    // -------------------------------------------------------------------------
    // Game-level logic
    // -------------------------------------------------------------------------

    /**
     * Returns the winning [Team] if the point just scored ends the game, or null otherwise.
     * Called BEFORE updating the game state.
     */
    private fun gameWinner(
        game: GameState,
        ruleMode: RuleMode,
        scoringTeam: Team,
        newYou: Int,
        newOpp: Int
    ): Team? {
        return when (game.phase) {
            GamePhase.NORMAL -> {
                if (game.mode == GameMode.TIEBREAK) {
                    val (scorer, other) = if (scoringTeam == Team.YOU) Pair(newYou, newOpp) else Pair(newOpp, newYou)
                    if (scorer >= 7 && scorer - other >= 2) scoringTeam else null
                } else {
                    val (scorer, other) = if (scoringTeam == Team.YOU) Pair(newYou, newOpp) else Pair(newOpp, newYou)
                    // Win if scorer reaches 4+ with 2+ lead, excluding the 3:3 deuce path
                    if (scorer >= 4 && scorer - other >= 2) scoringTeam else null
                }
            }
            GamePhase.ADV_YOU       -> if (scoringTeam == Team.YOU) Team.YOU else null
            GamePhase.ADV_OPP       -> if (scoringTeam == Team.OPP) Team.OPP else null
            GamePhase.GOLDEN        -> scoringTeam
            GamePhase.STAR_ADV_YOU  -> if (scoringTeam == Team.YOU) Team.YOU else null
            GamePhase.STAR_ADV_OPP  -> if (scoringTeam == Team.OPP) Team.OPP else null
            GamePhase.STAR_POINT    -> scoringTeam
            GamePhase.DEUCE         -> null
        }
    }

    /**
     * Returns the new (GamePhase, starAdvCount) after a scored point that did NOT end the game.
     */
    private fun nextGamePhase(
        game: GameState,
        ruleMode: RuleMode,
        scoringTeam: Team,
        newYou: Int,
        newOpp: Int
    ): Pair<GamePhase, Int> {
        val advCount = game.starAdvCount
        return when (game.phase) {
            GamePhase.NORMAL -> {
                // Tie-break never enters deuce/advantage territory — raw points keep climbing
                if (game.mode == GameMode.TIEBREAK) {
                    Pair(GamePhase.NORMAL, advCount)
                } else if (newYou == 3 && newOpp == 3) {
                    when (ruleMode) {
                        RuleMode.STANDARD -> Pair(GamePhase.DEUCE, advCount)
                        RuleMode.GOLDEN   -> Pair(GamePhase.GOLDEN, advCount)
                        RuleMode.STAR     -> Pair(GamePhase.DEUCE, advCount)
                    }
                } else {
                    Pair(GamePhase.NORMAL, advCount)
                }
            }
            GamePhase.DEUCE -> {
                val newPhase = when {
                    scoringTeam == Team.YOU -> if (ruleMode == RuleMode.STAR) GamePhase.STAR_ADV_YOU else GamePhase.ADV_YOU
                    else                   -> if (ruleMode == RuleMode.STAR) GamePhase.STAR_ADV_OPP else GamePhase.ADV_OPP
                }
                Pair(newPhase, advCount)
            }
            // Standard advantage lost → back to deuce
            GamePhase.ADV_YOU, GamePhase.ADV_OPP -> Pair(GamePhase.DEUCE, advCount)
            // Star advantage lost → increment advCount → deuce or star point
            GamePhase.STAR_ADV_YOU, GamePhase.STAR_ADV_OPP -> {
                val newAdvCount = advCount + 1
                val newPhase = if (newAdvCount >= 2) GamePhase.STAR_POINT else GamePhase.DEUCE
                Pair(newPhase, newAdvCount)
            }
            // These phases always produce a winner (handled above), so this is unreachable
            GamePhase.GOLDEN, GamePhase.STAR_POINT -> Pair(GamePhase.NORMAL, advCount)
        }
    }

    // -------------------------------------------------------------------------
    // Game won → set level
    // -------------------------------------------------------------------------

    private fun applyGameWin(snapshot: Snapshot, winner: Team, stats: StatsState): Snapshot {
        // Track service breaks (regular games only; tiebreak wins don't count as breaks)
        val updatedStats = if (snapshot.game.mode == GameMode.REGULAR) {
            when {
                winner == Team.YOU && snapshot.serve.serverTeam == Team.OPP ->
                    stats.copy(breaksYou = stats.breaksYou + 1)
                winner == Team.OPP && snapshot.serve.serverTeam == Team.YOU ->
                    stats.copy(breaksOpp = stats.breaksOpp + 1)
                else -> stats
            }
        } else stats

        val newYouGames = snapshot.set.youGames + if (winner == Team.YOU) 1 else 0
        val newOppGames = snapshot.set.oppGames + if (winner == Team.OPP) 1 else 0

        // Check for tie-break trigger
        if (newYouGames == 6 && newOppGames == 6) {
            return startTieBreak(snapshot, updatedStats)
        }

        // Check for set win
        val setWinner = setWinner(newYouGames, newOppGames)
        return if (setWinner != null) {
            applySetWin(snapshot, setWinner, newYouGames, newOppGames, updatedStats)
        } else {
            // Advance game, rotate server
            val nextOrderIndex = (snapshot.serve.serveOrderIndex + 1) % snapshot.config.serveOrder.size
            val nextPlayer = snapshot.config.serveOrder[nextOrderIndex]
            val newServe = ServeState(
                serverTeam = nextPlayer.team(),
                serverPlayer = nextPlayer,
                serveSide = ServeSide.RIGHT,
                serveOrderIndex = nextOrderIndex
            )
            snapshot.copy(
                set = snapshot.set.copy(youGames = newYouGames, oppGames = newOppGames),
                game = resetGame(GameMode.REGULAR),
                serve = newServe,
                stats = updatedStats
            )
        }
    }

    private fun startTieBreak(snapshot: Snapshot, stats: StatsState): Snapshot {
        // TB is served by the NEXT player in the rotation (who would have served game 13)
        val tbOrderIndex = (snapshot.serve.serveOrderIndex + 1) % snapshot.config.serveOrder.size
        val tbPlayer = snapshot.config.serveOrder[tbOrderIndex]
        val newServe = ServeState(
            serverTeam = tbPlayer.team(),
            serverPlayer = tbPlayer,
            serveSide = ServeSide.RIGHT,
            serveOrderIndex = tbOrderIndex
        )
        return snapshot.copy(
            set = snapshot.set.copy(youGames = 6, oppGames = 6),
            game = resetGame(GameMode.TIEBREAK),
            serve = newServe,
            stats = stats
        )
    }

    /** Set win condition: first to 6 with 2-game lead, or 7-5 after extension. */
    private fun setWinner(you: Int, opp: Int): Team? {
        return when {
            you >= 6 && you - opp >= 2 -> Team.YOU
            opp >= 6 && opp - you >= 2 -> Team.OPP
            // After TB: 7-6
            you == 7 && opp == 6       -> Team.YOU
            opp == 7 && you == 6       -> Team.OPP
            else                       -> null
        }
    }

    // -------------------------------------------------------------------------
    // Set won → match level
    // -------------------------------------------------------------------------

    private fun applySetWin(
        snapshot: Snapshot,
        winner: Team,
        youGames: Int,
        oppGames: Int,
        stats: StatsState
    ): Snapshot {
        val newSetsYou = snapshot.match.setsWonYou + if (winner == Team.YOU) 1 else 0
        val newSetsOpp = snapshot.match.setsWonOpp + if (winner == Team.OPP) 1 else 0

        val completedSetScore = SetScore(youGames, oppGames, isCompleted = true)
        val updatedSetScores = snapshot.match.setScores.dropLast(1) + completedSetScore

        val setsToWin = (snapshot.config.bestOf + 1) / 2
        val matchOver = newSetsYou >= setsToWin || newSetsOpp >= setsToWin

        val newMatch = snapshot.match.copy(
            setsWonYou = newSetsYou,
            setsWonOpp = newSetsOpp,
            setScores = updatedSetScores + if (!matchOver) listOf(SetScore(0, 0, false)) else emptyList()
        )

        if (matchOver) {
            return snapshot.copy(match = newMatch, isMatchOver = true, stats = stats)
        }

        // Start new set
        val nextOrderIndex = (snapshot.serve.serveOrderIndex + 1) % snapshot.config.serveOrder.size
        val nextPlayer = snapshot.config.serveOrder[nextOrderIndex]
        val newServe = ServeState(
            serverTeam = nextPlayer.team(),
            serverPlayer = nextPlayer,
            serveSide = ServeSide.RIGHT,
            serveOrderIndex = nextOrderIndex
        )
        return snapshot.copy(
            match = newMatch,
            set = SetState(currentSetIndex = newSetsYou + newSetsOpp, youGames = 0, oppGames = 0),
            game = resetGame(GameMode.REGULAR),
            serve = newServe,
            stats = stats
        )
    }

    // -------------------------------------------------------------------------
    // Serve helpers
    // -------------------------------------------------------------------------

    /** Even rally index → RIGHT, odd → LEFT. Applies to both regular games and tie-breaks. */
    private fun serveSideFromRallyIndex(rallyIndex: Int): ServeSide =
        if (rallyIndex % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT

    /**
     * Returns the index into [Config.serveOrder] for the server at [tbPointIndex] in a tie-break.
     *
     * Rule: start server serves 1 point (index 0), then each subsequent server serves 2 points.
     * startOrderIndex = the index of the player who opens the tie-break.
     */
    fun tieBreakServerOrderIndex(tbPointIndex: Int, startOrderIndex: Int, orderSize: Int): Int {
        if (tbPointIndex == 0) return startOrderIndex
        val remaining = tbPointIndex - 1
        val block = remaining / 2
        return (startOrderIndex + 1 + block) % orderSize
    }

    /** Recompute serve state mid-tie-break (called after each TB point). */
    private fun tieBreakServe(snapshot: Snapshot, newYou: Int, newOpp: Int): ServeState {
        val tbPointIndex = newYou + newOpp
        val startOrderIndex = snapshot.serve.serveOrderIndex
        val order = snapshot.config.serveOrder
        val serverIdx = tieBreakServerOrderIndex(tbPointIndex, startOrderIndex, order.size)
        val player = order[serverIdx]
        return ServeState(
            serverTeam = player.team(),
            serverPlayer = player,
            serveSide = serveSideFromRallyIndex(tbPointIndex),
            serveOrderIndex = startOrderIndex // TB start index never changes within the TB
        )
    }

    // -------------------------------------------------------------------------
    // Game-level pill helpers
    // -------------------------------------------------------------------------

    /** True if [team] can win the current game with the next single point. */
    private fun canWinGameNextPoint(game: GameState, team: Team): Boolean {
        return when (game.phase) {
            GamePhase.NORMAL -> {
                if (game.mode == GameMode.TIEBREAK) {
                    val (you, opp) = if (team == Team.YOU) Pair(game.youPoints, game.oppPoints)
                                     else Pair(game.oppPoints, game.youPoints)
                    you + 1 >= 7 && (you + 1) - opp >= 2
                } else {
                    val (you, opp) = if (team == Team.YOU) Pair(game.youPoints, game.oppPoints)
                                     else Pair(game.oppPoints, game.youPoints)
                    you == 3 && opp < 3 // 40-0, 40-15, 40-30 (not 40-40 which goes to deuce)
                }
            }
            GamePhase.ADV_YOU       -> team == Team.YOU
            GamePhase.ADV_OPP       -> team == Team.OPP
            GamePhase.GOLDEN        -> true
            GamePhase.STAR_ADV_YOU  -> team == Team.YOU
            GamePhase.STAR_ADV_OPP  -> team == Team.OPP
            GamePhase.STAR_POINT    -> true
            GamePhase.DEUCE         -> false
        }
    }

    /**
     * True if winning the next game in [set] would win the current set for [team].
     * Takes the TB scenario (6:6) into account.
     */
    private fun canWinSetNextGame(set: SetState, match: MatchState, team: Team): Boolean {
        val (you, opp) = if (team == Team.YOU) Pair(set.youGames, set.oppGames)
                         else Pair(set.oppGames, set.youGames)
        val afterWin = you + 1
        return setWinner(
            if (team == Team.YOU) afterWin else set.youGames,
            if (team == Team.OPP) afterWin else set.oppGames
        ) == team
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private fun resetGame(mode: GameMode) = GameState(
        mode = mode,
        youPoints = 0,
        oppPoints = 0,
        phase = GamePhase.NORMAL,
        starAdvCount = 0
    )
}
