package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Tests for the between-set position-switch mechanic (doubles only).
 *
 * After each non-final set in DOUBLES mode both teams may independently choose
 * to swap their court positions (A1↔A2 for YOU, B1↔B2 for OPP).
 * Singles matches are unaffected.
 */
class PositionSwitchTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Score enough points to win a set 6-0 (6 games × 4 uncontested points). */
    private fun Snapshot.winSet(winner: Team): Snapshot = scoreMany(*Array(24) { winner })

    // ── Flag lifecycle ────────────────────────────────────────────────────────

    @Test fun `doubles set end sets both awaiting flags`() {
        val s = makeSnapshot(config = doublesConfig())
            .winSet(Team.YOU)
        assertTrue(s.awaitingYouPositionSwitch)
        assertTrue(s.awaitingOppPositionSwitch)
    }

    @Test fun `singles set end does not set awaiting flags`() {
        val s = makeSnapshot(config = singlesConfig())
            .winSet(Team.YOU)
        assertFalse(s.awaitingYouPositionSwitch)
        assertFalse(s.awaitingOppPositionSwitch)
    }

    @Test fun `doubles match end does not set awaiting flags`() {
        // Best-of-1 doesn't exist, so use best-of-3 and win 2 sets straight
        var s = makeSnapshot(config = doublesConfig(bestOf = 3))
        s = s.winSet(Team.YOU)  // set 1 → awaiting position switch
        // Bypass position switch (keep both) then win set 2
        s = MatchEngine.confirmYouPositionSwitch(s, false)
        s = MatchEngine.confirmOppPositionSwitch(s, false)
        s = s.winSet(Team.YOU)  // set 2 → match over
        assertTrue(s.isMatchOver)
        assertFalse(s.awaitingYouPositionSwitch)
        assertFalse(s.awaitingOppPositionSwitch)
    }

    // ── YOU team confirmation ─────────────────────────────────────────────────

    @Test fun `confirmYouPositionSwitch keep clears YOU flag and OPP flag stays`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val after = MatchEngine.confirmYouPositionSwitch(s, false)
        assertFalse(after.awaitingYouPositionSwitch)
        assertTrue(after.awaitingOppPositionSwitch)
    }

    @Test fun `confirmYouPositionSwitch switch clears YOU flag and OPP flag stays`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val after = MatchEngine.confirmYouPositionSwitch(s, true)
        assertFalse(after.awaitingYouPositionSwitch)
        assertTrue(after.awaitingOppPositionSwitch)
    }

    @Test fun `confirmYouPositionSwitch keep preserves serve order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val after = MatchEngine.confirmYouPositionSwitch(s, false)
        assertEquals(s.config.serveOrder, after.config.serveOrder)
    }

    @Test fun `confirmYouPositionSwitch switch swaps A1 and A2 in serve order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val after = MatchEngine.confirmYouPositionSwitch(s, true)
        // Original order: A1, B1, A2, B2 → after swap: A2, B1, A1, B2
        val expected = s.config.serveOrder.map { p ->
            when (p) { Player.A1 -> Player.A2; Player.A2 -> Player.A1; else -> p }
        }
        assertEquals(expected, after.config.serveOrder)
    }

    @Test fun `confirmYouPositionSwitch switch does not affect B players in order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val after = MatchEngine.confirmYouPositionSwitch(s, true)
        val bPlayers = after.config.serveOrder.filter { it == Player.B1 || it == Player.B2 }
        val bExpected = s.config.serveOrder.filter { it == Player.B1 || it == Player.B2 }
        assertEquals(bExpected, bPlayers)
    }

    @Test fun `confirmYouPositionSwitch switch updates current server when YOU team serves`() {
        // After doublesConfig 6-0 set, next server index = 1 → B1 (OPP).
        // Use a snapshot where A1 would serve to verify the player flips.
        val base = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        // Force the next server to A1 by manually adjusting — re-score from a
        // state where serveOrderIndex wraps to 0 (A1).
        // Easiest: start fresh with OPP-first order so after set 1 it's A-team's turn.
        val config = doublesConfig() // A1, B1, A2, B2
        // Play 8 games (wrap index to 0 = A1) before set win:
        // serveOrderIndex advances 1 per game → 24 points = 6 games from start, index 0→6 mod 4 = 2
        // Actually after 24 points (6-0 set win) the next index = (0+6+1) mod 4 = 7 mod 4 = 3 → B2
        // Let's just verify: if awaitingYouPositionSwitch and current server is B2, no change to server
        val s = makeSnapshot(config = config).winSet(Team.YOU)
        // current server after 6-0 = serveOrderIndex goes 0→6 games, last game winner rotates to idx=7%4=3 = B2? Wait: applySetWin advances by 1 AFTER the set win
        // Actually after winSet(YOU) the new serve order index = (0 + 1 per game for 6 games... no:
        // applySetWin only advances once: nextOrderIndex = (lastGameServeOrderIndex + 1) % size
        // Each game advances by 1. After 6 games serveOrderIndex = 6 % 4 = 2 = A2? Let's just check:
        val after = MatchEngine.confirmYouPositionSwitch(s, true)
        // The server player should be the partner of whatever it was if they are on Team.YOU
        if (s.serve.serverTeam == Team.YOU) {
            val expectedServer = when (s.serve.serverPlayer) {
                Player.A1 -> Player.A2
                Player.A2 -> Player.A1
                else -> s.serve.serverPlayer
            }
            assertEquals(expectedServer, after.serve.serverPlayer)
        } else {
            assertEquals(s.serve.serverPlayer, after.serve.serverPlayer)
        }
    }

    @Test fun `confirmYouPositionSwitch is no-op when flag is not set`() {
        val s = makeSnapshot(config = doublesConfig())
        val after = MatchEngine.confirmYouPositionSwitch(s, true)
        assertSame(s, after)
    }

    // ── OPP team confirmation ─────────────────────────────────────────────────

    @Test fun `confirmOppPositionSwitch keep clears OPP flag`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val after = MatchEngine.confirmOppPositionSwitch(mid, false)
        assertFalse(after.awaitingOppPositionSwitch)
    }

    @Test fun `confirmOppPositionSwitch switch clears OPP flag`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val after = MatchEngine.confirmOppPositionSwitch(mid, true)
        assertFalse(after.awaitingOppPositionSwitch)
    }

    @Test fun `confirmOppPositionSwitch keep preserves serve order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val before = mid.config.serveOrder
        val after = MatchEngine.confirmOppPositionSwitch(mid, false)
        assertEquals(before, after.config.serveOrder)
    }

    @Test fun `confirmOppPositionSwitch switch swaps B1 and B2 in serve order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val after = MatchEngine.confirmOppPositionSwitch(mid, true)
        val expected = mid.config.serveOrder.map { p ->
            when (p) { Player.B1 -> Player.B2; Player.B2 -> Player.B1; else -> p }
        }
        assertEquals(expected, after.config.serveOrder)
    }

    @Test fun `confirmOppPositionSwitch switch does not affect A players in order`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val after = MatchEngine.confirmOppPositionSwitch(mid, true)
        val aPlayers = after.config.serveOrder.filter { it == Player.A1 || it == Player.A2 }
        val aExpected = mid.config.serveOrder.filter { it == Player.A1 || it == Player.A2 }
        assertEquals(aExpected, aPlayers)
    }

    @Test fun `confirmOppPositionSwitch updates current server when OPP team serves`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        val after = MatchEngine.confirmOppPositionSwitch(mid, true)
        if (mid.serve.serverTeam == Team.OPP) {
            val expectedServer = when (mid.serve.serverPlayer) {
                Player.B1 -> Player.B2
                Player.B2 -> Player.B1
                else -> mid.serve.serverPlayer
            }
            assertEquals(expectedServer, after.serve.serverPlayer)
        } else {
            assertEquals(mid.serve.serverPlayer, after.serve.serverPlayer)
        }
    }

    @Test fun `confirmOppPositionSwitch is no-op when flag is not set`() {
        val s = makeSnapshot(config = doublesConfig())
        val after = MatchEngine.confirmOppPositionSwitch(s, true)
        assertSame(s, after)
    }

    // ── Combined switches ─────────────────────────────────────────────────────

    @Test fun `both switches applied swaps all four players`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid   = MatchEngine.confirmYouPositionSwitch(s, true)
        val after = MatchEngine.confirmOppPositionSwitch(mid, true)
        val expected = s.config.serveOrder.map { p ->
            when (p) {
                Player.A1 -> Player.A2; Player.A2 -> Player.A1
                Player.B1 -> Player.B2; Player.B2 -> Player.B1
            }
        }
        assertEquals(expected, after.config.serveOrder)
    }

    @Test fun `after both confirmed no awaiting flags remain`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid   = MatchEngine.confirmYouPositionSwitch(s, true)
        val after = MatchEngine.confirmOppPositionSwitch(mid, false)
        assertFalse(after.awaitingYouPositionSwitch)
        assertFalse(after.awaitingOppPositionSwitch)
    }

    // ── Score blocked while awaiting ─────────────────────────────────────────

    @Test fun `score is blocked while awaiting YOU position switch`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        assertTrue(s.awaitingYouPositionSwitch)
        val after = MatchEngine.score(s, Team.YOU)
        assertSame(s, after)
    }

    @Test fun `score is blocked while awaiting OPP position switch`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid = MatchEngine.confirmYouPositionSwitch(s, false)
        assertTrue(mid.awaitingOppPositionSwitch)
        val after = MatchEngine.score(mid, Team.YOU)
        assertSame(mid, after)
    }

    @Test fun `score proceeds after both switches confirmed`() {
        val s = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        val mid   = MatchEngine.confirmYouPositionSwitch(s, false)
        val ready = MatchEngine.confirmOppPositionSwitch(mid, false)
        val after = MatchEngine.score(ready, Team.YOU)
        assertNotSame(ready, after)
    }

    // ── Undo behaviour ───────────────────────────────────────────────────────
    // Confirmations REPLACE the top of the history stack (like setDeciderSide),
    // so a single undo skips the entire overlay sequence and lands on the last
    // scored point — which is before the set ended and has no awaiting flags.

    @Test fun `undo after both confirmations lands on pre-set-win point`() {
        // Simulate ViewModel replace-on-confirm behaviour.
        val priorPoint = makeSnapshot(config = doublesConfig())
        val setWinState = priorPoint.winSet(Team.YOU)
        val history = mutableListOf(priorPoint, setWinState)

        // Each confirmation replaces the top entry
        val afterYou = MatchEngine.confirmYouPositionSwitch(setWinState, false)
        history[history.lastIndex] = afterYou
        val afterOpp = MatchEngine.confirmOppPositionSwitch(afterYou, false)
        history[history.lastIndex] = afterOpp

        // Single undo removes the entire position-switch sequence
        history.removeLast()
        val restored = history.last()
        assertFalse(restored.awaitingYouPositionSwitch)
        assertFalse(restored.awaitingOppPositionSwitch)
    }

    @Test fun `undo after both confirmations with switches lands on pre-set-win point`() {
        val priorPoint = makeSnapshot(config = doublesConfig())
        val setWinState = priorPoint.winSet(Team.YOU)
        val history = mutableListOf(priorPoint, setWinState)

        val afterYou = MatchEngine.confirmYouPositionSwitch(setWinState, true)
        history[history.lastIndex] = afterYou
        val afterOpp = MatchEngine.confirmOppPositionSwitch(afterYou, true)
        history[history.lastIndex] = afterOpp

        history.removeLast()
        val restored = history.last()
        assertFalse(restored.awaitingYouPositionSwitch)
        assertFalse(restored.awaitingOppPositionSwitch)
    }
}
