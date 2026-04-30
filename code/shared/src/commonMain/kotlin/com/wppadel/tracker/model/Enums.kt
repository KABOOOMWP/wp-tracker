package com.wppadel.tracker.model

enum class Team { YOU, OPP }

enum class Player { A1, A2, B1, B2 }

fun Player.team(): Team = if (this == Player.A1 || this == Player.A2) Team.YOU else Team.OPP

enum class ServeSide { LEFT, RIGHT }

enum class PlayMode { SINGLES, DOUBLES }

enum class RuleMode { STANDARD, GOLDEN, STAR }

enum class GameMode { REGULAR, TIEBREAK }

/**
 * Phase of the current game point state.
 *
 * NORMAL      – standard play (0/15/30/40 territory, no deuce yet)
 * DEUCE       – both at 40 (displayed "40 40"), no advantage yet (or returned to deuce)
 * ADV_YOU     – You have advantage (Standard mode) → displayed "AD 40"
 * ADV_OPP     – Opponent has advantage (Standard mode) → displayed "40 AD"
 * GOLDEN      – Golden Point deciding point active
 * STAR_ADV_YOU– Star mode: You have advantage (first or second)
 * STAR_ADV_OPP– Star mode: Opponent has advantage
 * STAR_POINT  – Star mode: third deuce reached, deciding point active
 */
enum class GamePhase {
    NORMAL,
    DEUCE,
    ADV_YOU, ADV_OPP,
    GOLDEN,
    STAR_ADV_YOU, STAR_ADV_OPP,
    STAR_POINT
}

/** What the status pill shows. Priority enforced by MatchEngine. */
enum class PillState {
    HIDDEN,
    TIEBREAK,
    GOLDEN_POINT,
    STAR_POINT,
    BREAK_POINT,
    SET_POINT,
    MATCH_POINT
}

/**
 * One of the four corners a score number can occupy on the watch face.
 * Spec: even rally index → You bottom-right / Opp top-left. Odd → You bottom-left / Opp top-right.
 */
enum class ScorePosition { BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT }

/** Diagonal layout of the two score numbers derived from the current rally index. */
data class ScoreLayout(val youPosition: ScorePosition, val oppPosition: ScorePosition)
